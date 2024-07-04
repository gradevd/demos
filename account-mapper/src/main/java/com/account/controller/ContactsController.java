package com.account.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.account.entity.CreateContactTaskEntity;
import com.account.error.DuplicateTaskException;
import com.account.error.ErrorResponse;
import com.account.service.CreateContactTaskService;

@RestController
@RequestMapping("contacts")
public class ContactsController {

   private final Logger _logger = LoggerFactory.getLogger(getClass());
   private final CreateContactTaskService _createContactTaskService;

   @Autowired
   public ContactsController(
         final CreateContactTaskService createContactTaskService) {
      _createContactTaskService = createContactTaskService;
   }

   @PostMapping
   public ResponseEntity<?> create(
         @RequestBody final CreateContactRequestBody requestBody) {
      _logger.debug(
            "Received a request to create a Freshdesk contact for account {}@{}.",
            requestBody.account, requestBody.origin);
      final CreateContactTaskEntity task;
      try {
         task = _createContactTaskService.create(requestBody.account,
               requestBody.origin);
         return ResponseEntity.status(HttpStatus.CREATED).body(task);
      } catch (final DuplicateTaskException e) {
         return ResponseEntity.status(HttpStatus.CONFLICT)
               .body(new ErrorResponse("Duplicate task.", e.getMessage()));
      } catch (final IllegalArgumentException e) {
         return ResponseEntity.badRequest()
               .body(new ErrorResponse("Bad request parameters.",
                     e.getMessage()));
      } catch (final RuntimeException e) {
         _logger.error("An unexpected error occurred.", e);
         return ResponseEntity.badRequest()
               .body(new ErrorResponse("Bad request parameters.",
                     "Something went wrong. Please contact the support team for further assistance."));
      }
   }

   record CreateContactRequestBody(String account, String origin) {
   }
}
