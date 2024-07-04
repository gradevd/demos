package com.account.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.account.constants.Constants;
import com.account.entity.CreateContactTaskEntity;
import com.account.error.DuplicateTaskException;
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

   @Autowired
   public CreateContactTaskService(
         final CreateContactTaskRepository createContactTaskRepository) {
      _createContactTaskRepository = createContactTaskRepository;
   }

   /**
    * Creates a new task entity in the DB layer.
    *
    * @param account - The account name to create a task for.
    * @param origin  - The account origin.
    * @return The newly created task in case of no conflict with a duplicate
    * pending task.
    * @throws IllegalArgumentException when null, or empty account, or account
    *                                  origin, or an unsupported account origin
    *                                  are provided.
    * @throws DuplicateTaskException   when a duplicate pending task is being
    *                                  created.
    */
   public CreateContactTaskEntity create(final String account,
         final String origin) {
      // Empty account
      if (account == null || account.isBlank()) {
         final String message = "Account name must not be null or empty.";
         _logger.error(message);
         throw new IllegalArgumentException(
               "Account name must not be null or empty.");
      }
      // Empty origin
      if (origin == null || origin.isBlank()) {
         final String message = "Account origin must not be null or empty.";
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
      // Duplicate task
      final Optional<CreateContactTaskEntity> optional = _createContactTaskRepository.findByAccountAndAccountOrigin(
            account, accountOrigin);
      if (optional.isPresent()) {
         final CreateContactTaskEntity existingTask = optional.get();
         if (!existingTask.hasCompleted()) {
            final String message = String.format(
                  "A duplicate pending task %s already exists.", existingTask);
            _logger.error(message);
            throw new DuplicateTaskException(message);
         }
      }
      // Create a new task
      final CreateContactTaskEntity task = _createContactTaskRepository.save(
            new CreateContactTaskEntity(account, accountOrigin));
      _logger.debug("Successfully created task {}.", task);
      return task;
   }
}
