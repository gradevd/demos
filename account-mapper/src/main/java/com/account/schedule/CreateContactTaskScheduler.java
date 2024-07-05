package com.account.schedule;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.account.constants.Constants;
import com.account.entity.CreateContactTaskEntity;
import com.account.repository.CreateContactTaskRepository;
import com.account.service.CreateContactTaskService;

/**
 * The scheduler component responsible for scheduling of tasks for
 * contacts creation and completed task cleanup.
 */
@Component
public class CreateContactTaskScheduler {

   private final Logger _logger = LoggerFactory.getLogger(getClass());
   private final CreateContactTaskRepository _contactTaskRepository;
   private final CreateContactTaskService _createContactTaskService;

   private final int _maxAttempts;
   private final long _initialTimeoutValueMillis;
   private final long _timeoutStepMillis;

   @Autowired
   public CreateContactTaskScheduler(
         final CreateContactTaskRepository contactTaskRepository,
         final CreateContactTaskService createContactTaskService,
         @Value("${create.contact.task.max.attempts}") final int maxAttempts,
         @Value("${create.contact.task.timeout.initial.value.millis}") final long initialTimeoutValueMillis,
         @Value("${create.contact.task.timeout.step.millis}") final long timeoutStepMillis) {
      _contactTaskRepository = contactTaskRepository;
      _createContactTaskService = createContactTaskService;
      _maxAttempts = maxAttempts;
      _initialTimeoutValueMillis = initialTimeoutValueMillis;
      _timeoutStepMillis = timeoutStepMillis;
   }

   /**
    * Every {@code fixedRateString} collects all tasks that have not started
    * yet, or that must be retried and have exceeded the configured timeout
    * for that.
    * Posts the eligible tasks for execution to the pre-defined
    * {@code ExecutorService}.
    */
   @Scheduled(scheduler = "taskScheduler", fixedRateString = "${create.contact.task.scheduler.rate.millis}")
   public void scheduleTasks() {
      _logger.debug("Scanning for pending tasks.");
      final List<CreateContactTaskEntity> pendingTasks = _contactTaskRepository.findByStatusInOrderByUpdatedAsc(
            Arrays.asList(Constants.CreateContactTaskStatus.TO_RETRY,
                  Constants.CreateContactTaskStatus.NOT_STARTED));
      final List<CreateContactTaskEntity> eligibleTasksToRetry = pendingTasks.stream()
            .filter(this::hasReachedTimeoutOfCurrentAttempt).toList();
      if (eligibleTasksToRetry.isEmpty()) {
         _logger.debug("No pending tasks found.");
      } else {
         _logger.debug("Found {} executable tasks.\n{}.",
               eligibleTasksToRetry.size(), eligibleTasksToRetry);
         for (final CreateContactTaskEntity task : eligibleTasksToRetry) {
            _createContactTaskService.execute(task.id);
         }
      }
   }

   /**
    * Every {@code fixedRateString} collects all tasks that have completed, or
    * that have exceeded the pre-configured {@code maxAttempts} number,
    * Cleans up the eligible tasks from the {@link CreateContactTaskRepository}.
    */
   @Scheduled(scheduler = "taskCleaner", fixedRateString = "${create.contact.task.cleaner.rate.millis}")
   public void cleanupTasks() {
      _logger.trace("Cleaning up completed, or timed out tasks.");
      final List<CreateContactTaskEntity> tasks = _contactTaskRepository.findByStatusInOrAttemptsGreaterThanEqual(
            Arrays.asList(Constants.CreateContactTaskStatus.FAILED,
                  Constants.CreateContactTaskStatus.COMPLETED), _maxAttempts);
      if (tasks.isEmpty()) {
         _logger.debug("No completed tasks found.");
      } else {
         _logger.debug("Found {} completed tasks to cleanup.\n{}.",
               tasks.size(), tasks);
         _contactTaskRepository.deleteAll(tasks);
      }
   }

   /**
    * Determines whether the provided {@link CreateContactTaskEntity} has reached
    * the execution timeout of its current attempt.
    *
    * @param task - The task entity to test the timeout for.
    * @return Whether the task has reached its current attmept timeout.
    */
   private boolean hasReachedTimeoutOfCurrentAttempt(
         final CreateContactTaskEntity task) {
      final long attempts = task.attempts;
      if (attempts >= _maxAttempts) {
         _logger.debug(
               "Task {} has already exceeded the configured attempts limit of {}.",
               task, _maxAttempts);
         return false;
      }
      final long lastModified = task.updated.getTime();
      final long currentTimeoutForTask =
            _initialTimeoutValueMillis + (_timeoutStepMillis * (attempts - 1));
      final Date now = new Date();
      if (lastModified + currentTimeoutForTask <= now.getTime()) {
         _logger.debug(
               "Task {} has ALREADY reached its current timeout (millis) of {} for attempt {} as of now {}.",
               task, currentTimeoutForTask, attempts, now);
         return true;
      } else {
         _logger.debug(
               "Task {} still has NOT reached its current timeout (millis) of {} for attempts {} as of now {}.",
               task, currentTimeoutForTask, attempts, now);
         return false;
      }
   }
}
