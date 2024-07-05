package com.account.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import com.account.constants.Constants;
import com.account.entity.CreateContactTaskEntity;
import com.account.error.DuplicateTaskException;
import com.account.freshdesk.FreshdeskContactInfo;
import com.account.freshdesk.FreshdeskContactService;
import com.account.freshdesk.FreshdeskContactSpec;
import com.account.github.GitHubAccountService;
import com.account.github.GithubAccountInfo;
import com.account.repository.CreateContactTaskRepository;

/**
 * A service for managing the creation and execution of contact tasks.
 * </p>
 * The service takes care of the business logic to create task entities in the
 * DB layer and asynchronously execute assigned tasks.
 */
@Service
public class CreateContactTaskService {

   private final Logger _logger = LoggerFactory.getLogger(getClass());
   private final CreateContactTaskRepository _createContactTaskRepository;
   private final GitHubAccountService _gitHubAccountService;
   private final FreshdeskContactService _freshdeskContactService;

   @Autowired
   public CreateContactTaskService(
         final CreateContactTaskRepository createContactTaskRepository,
         final GitHubAccountService gitHubAccountService,
         final FreshdeskContactService freshdeskContactService) {
      _createContactTaskRepository = createContactTaskRepository;
      _gitHubAccountService = gitHubAccountService;
      _freshdeskContactService = freshdeskContactService;
   }

   /**
    * Creates a new task entity in the DB layer.
    *
    * @param account         - The account name to create a task for.
    * @param origin          - The account origin.
    * @param freshdeskDomain - The Freshdesk subdomain for account creation.
    * @return The newly created task in case of no conflict with a duplicate
    * pending task.
    * @throws IllegalArgumentException when null, or empty account, or account
    *                                  origin, or an unsupported account origin
    *                                  are provided.
    * @throws DuplicateTaskException   when a duplicate pending task is being
    *                                  created.
    */
   public CreateContactTaskEntity create(final String account,
         final String origin, final String freshdeskDomain) {
      // Empty account
      if (account == null || account.isBlank()) {
         final String message = "The account must not be null or empty.";
         _logger.error(message);
         throw new IllegalArgumentException(
               "Account name must not be null or empty.");
      }
      // Empty origin
      if (origin == null || origin.isBlank()) {
         final String message = "The account origin must not be null or empty.";
         _logger.error(message);
         throw new IllegalArgumentException(message);
      }
      // Empty Freshdesk subdomain
      if (freshdeskDomain == null || freshdeskDomain.isBlank()) {
         final String message = "The Freshdesk subdomain must not be null or empty.";
         _logger.error(message);
         throw new IllegalArgumentException(message);
      }
      // Unsupported origin
      final Constants.AccountOrigin accountOrigin;
      try {
         accountOrigin = Constants.AccountOrigin.valueOf(origin.toUpperCase());
      } catch (final IllegalArgumentException e) {
         final String message = String.format(
               "Unsupported account origin provided. Must be one of %s.",
               List.of(Constants.AccountOrigin.values()));
         _logger.error(message);
         throw new IllegalArgumentException(message);
      }
      // Duplicate task check
      final List<CreateContactTaskEntity> matchingTasks = _createContactTaskRepository.findByAccountAndAccountOriginAndFreshdeskDomain(
            account, accountOrigin, freshdeskDomain);
      _logger.debug("All matching tasks for {} @ {}: {}", account,
            accountOrigin, matchingTasks);
      if (matchingTasks.stream()
            .anyMatch(CreateContactTaskEntity::hasNotCompleted)) {
         final String message = "A duplicate pending task already exists.";
         _logger.error(message);
         throw new DuplicateTaskException(message);
      }
      // Create a new task
      final CreateContactTaskEntity task = _createContactTaskRepository.save(
            new CreateContactTaskEntity(account, accountOrigin,
                  freshdeskDomain));
      _logger.debug("Successfully created task {}.", task);
      return task;
   }

   /**
    * Asynchronously executes a task by a given ID.
    * The method assigns a task in a synchronized way in order to eliminate the
    * chance of multiple threads to work on a single task concurrently.
    * <p>
    * If some operation fails in a {@code Recoverable} way (e.g A remote API
    * call times out, or the API is currently not responding), the task moves to
    * {@code TO_RETRY} state and the task attempts are incremented.
    * The task will be scheduled for execution again following the configured
    * progressive timeout logic.
    *
    * @param taskId - The task ID to assign for execution.
    */
   @Async
   public void execute(final String taskId) {
      _logger.info(
            "Asynchronously executing a FreshdeskCreateContact task with ID '{}'.",
            taskId);
      CreateContactTaskEntity task = assignTask(taskId);
      if (task == null) {
         // Task is already assigned, or does not exist.
         return;
      }
      try {
         final GithubAccountInfo gitHubAccountInfo = getGitHubUserInfo(
               task.account);
         // Cache the github data
         task = updateFreshdeskContactTaskEntity(task, gitHubAccountInfo);
         createOrUpdateFreshdeskContact(task.freshdeskDomain,
               gitHubAccountInfo);
         task.status = Constants.CreateContactTaskStatus.COMPLETED;
      } catch (final RecoverableTaskException e) {
         _logger.debug(
               "CreateFreshdeskContact task {} failed due to a slow, or a not responding API. "
                     + "Will make another attempt to complete the task.",
               taskId);
         task.status = Constants.CreateContactTaskStatus.TO_RETRY;
      } catch (final Exception e) {
         _logger.debug(
               "CreateFreshdeskContact task {} failed in an unrecoverable way. "
                     + "The task will be dropped.", taskId);
         task.status = Constants.CreateContactTaskStatus.FAILED;
      } finally {
         _createContactTaskRepository.save(task);
      }
   }

   /**
    * Assigns a task for execution in a synchronized way in order to prevent
    * multiple threads taking the same task for execution.
    *
    * @param taskId The ID of the task to assign.
    * @return The assigned task, or null, if the task has already been assigned
    * to another thread.
    */
   private synchronized CreateContactTaskEntity assignTask(
         final String taskId) {
      final Optional<CreateContactTaskEntity> optional = _createContactTaskRepository.findById(
            taskId);
      if (optional.isEmpty()) {
         _logger.info("No task with ID '{}' found in the database.", taskId);
         return null;
      }
      final CreateContactTaskEntity task = optional.get();
      if (Constants.CreateContactTaskStatus.RUNNING.equals(task.status)) {
         _logger.info(
               "Task with ID {} is already assigned to another thread for execution.",
               taskId);
         return null;
      }
      task.status = Constants.CreateContactTaskStatus.RUNNING;
      task.attempts++;
      final CreateContactTaskEntity assignedTask = _createContactTaskRepository.save(
            task);
      _logger.info("Assigned task ID {} for execution.", task.id);
      return assignedTask;
   }

   /**
    * Retrieves information about the GitHub account using the configured API.
    *
    * @param account The GitHub account to lookup information for.
    * @return The retrieved {@link GithubAccountInfo}.
    * @throws RecoverableTaskException in case the API call fails in a
    *                                  recoverable way (e.g. service unavailable,
    *                                  or the API call times out).
    */
   private GithubAccountInfo getGitHubUserInfo(final String account) {
      try {
         return _gitHubAccountService.get(account);
      } catch (final HttpStatusCodeException e) {
         handleRecoverableException(e);
         if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
            _logger.info("Github Account '{}' does not exist.", account);
         }
         throw e;
      }
   }

   /**
    * Updates the running {@link CreateContactTaskEntity} with the
    * {@link GithubAccountInfo} retrieved by GitHub and persists the task.
    * If the task retries later on, it may use the cached {@link GithubAccountInfo}
    * information in case the GitHub API fails and still complete successfully.
    *
    * @param task              The task to update.
    * @param gitHubAccountInfo The {@link GithubAccountInfo} retrieved.
    * @return The updated {@link CreateContactTaskEntity}.
    */
   private CreateContactTaskEntity updateFreshdeskContactTaskEntity(
         final CreateContactTaskEntity task,
         final GithubAccountInfo gitHubAccountInfo) {
      task.status = Constants.CreateContactTaskStatus.RUNNING;
      task.externalAccountId = gitHubAccountInfo.id;
      task.address = gitHubAccountInfo.location;
      task.email = gitHubAccountInfo.email;
      return _createContactTaskRepository.save(task);
   }

   /**
    * Creates or updates an existing FreshdeskContact out of the retrieved
    * {@link GithubAccountInfo}.
    *
    * @param freshdeskDomain   The target Freshdesk domain.
    * @param gitHubAccountInfo The {@link GithubAccountInfo} to create the
    *                          contact from.
    * @throws RecoverableTaskException in case the API call fails in a
    *                                  recoverable way (e.g. service unavailable,
    *                                  or the API call times out).
    */
   private void createOrUpdateFreshdeskContact(final String freshdeskDomain,
         final GithubAccountInfo gitHubAccountInfo) {
      final FreshdeskContactSpec contactSpec = FreshdeskContactSpec.from(
            gitHubAccountInfo);
      final Optional<FreshdeskContactInfo> freshdeskContactInfo = _freshdeskContactService.findByExternalId(
            freshdeskDomain, contactSpec.uniqueExternalId);
      try {
         if (freshdeskContactInfo.isPresent()) {
            _freshdeskContactService.update(freshdeskDomain,
                  freshdeskContactInfo.get().id, contactSpec);
         } else {
            _freshdeskContactService.create(freshdeskDomain, contactSpec);
         }
      } catch (final HttpStatusCodeException e) {
         handleRecoverableException(e);
         throw e;
      }
   }

   private void handleRecoverableException(final HttpStatusCodeException e) {
      final HttpStatusCode statusCode = e.getStatusCode();
      if (statusCode.is5xxServerError()) {
         if (HttpStatus.BAD_GATEWAY.equals(statusCode)
               || HttpStatus.SERVICE_UNAVAILABLE.equals(statusCode)
               || HttpStatus.GATEWAY_TIMEOUT.equals(statusCode)) {
            throw new RecoverableTaskException();
         }
      }
   }

   private static class RecoverableTaskException extends RuntimeException {
   }
}
