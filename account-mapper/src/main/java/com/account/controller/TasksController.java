package com.account.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.account.entity.CreateContactTaskEntity;
import com.account.error.DuplicateTaskException;
import com.account.error.ErrorResponse;
import com.account.service.CreateContactTaskService;

@RestController
@RequestMapping("tasks")
public class TasksController {

   private final Logger _logger = LoggerFactory.getLogger(getClass());
   private final CreateContactTaskService _createContactTaskService;

   @Autowired
   public TasksController(
         final CreateContactTaskService createContactTaskService) {
      _createContactTaskService = createContactTaskService;
   }

   @PostMapping
   public ResponseEntity<?> create(
         @RequestBody final CreateContactRequestBody requestBody) {
      _logger.debug(
            "Received a request to create a {}.freshdesk.com contact for account {}@{}.",
            requestBody.freshdeskDomain, requestBody.account,
            requestBody.origin);
      try {
         final CreateContactTaskEntity task = _createContactTaskService.create(
               requestBody.account, requestBody.origin,
               requestBody.freshdeskDomain);
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

   @GetMapping
   public ResponseEntity<?> list() {
      return ResponseEntity.status(HttpStatus.OK)
            .body(_createContactTaskService.list());
   }

   /**
    * Represents the input parameters of the API.
    *
    * @param account         - the external account to get information from.
    * @param origin          - the origin of the external account.
    * @param freshdeskDomain - the Freshdesk subdomain to store the contact at.
    */
   record CreateContactRequestBody(String account, String origin,
                                   String freshdeskDomain) {
   }
}
