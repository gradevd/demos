package com.account.controller;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.account.constants.Constants;
import com.account.entity.CreateContactTaskEntity;
import com.account.error.DuplicateTaskException;
import com.account.error.ErrorResponse;
import com.account.service.CreateContactTaskService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class TasksControllerTest {

   @InjectMocks
   private TasksController _tasksController;

   @Mock
   private CreateContactTaskService _taskService;

   public TasksControllerTest() {
      MockitoAnnotations.openMocks(this);
   }

   @Test
   public void create_withCorrectArguments_returnsTheCreatedTask() {
      final String account = "test-account";
      final String origin = "github";
      final String freshdeskDomain = "my-freshworks";
      final CreateContactTaskEntity expectedTaskEntity = new CreateContactTaskEntity(
            account, Constants.AccountOrigin.valueOf(origin.toUpperCase()),
            freshdeskDomain);
      expectedTaskEntity.id = "1";

      when(_taskService.create(account, origin, freshdeskDomain)).thenReturn(
            expectedTaskEntity);

      final ResponseEntity<?> response = _tasksController.create(
            new TasksController.CreateContactRequestBody(account, origin,
                  freshdeskDomain));

      assertEquals(response.getStatusCode(), HttpStatus.CREATED);
      final CreateContactTaskEntity entity = (CreateContactTaskEntity) response.getBody();
      assertNotNull(entity);
      assertEquals(entity.id, expectedTaskEntity.id);
      assertEquals(entity.account, expectedTaskEntity.account);
      assertEquals(entity.accountOrigin, expectedTaskEntity.accountOrigin);
      assertEquals(entity.freshdeskDomain, expectedTaskEntity.freshdeskDomain);
   }

   @Test
   public void create_withCorrectArguments_whenDuplicateTaskExists_returnsConflictResponse() {
      final String account = "test-account";
      final String origin = "github";
      final String freshdeskDomain = "my-freshworks";

      final DuplicateTaskException exceptionThrown = new DuplicateTaskException(
            "test-message");
      when(_taskService.create(account, origin, freshdeskDomain)).thenThrow(
            exceptionThrown);

      final ResponseEntity<?> response = _tasksController.create(
            new TasksController.CreateContactRequestBody(account, origin,
                  freshdeskDomain));

      assertEquals(response.getStatusCode(), HttpStatus.CONFLICT);
      final ErrorResponse body = (ErrorResponse) response.getBody();
      assertNotNull(body);
      assertEquals(body.message(), "Duplicate task.");
      assertEquals(body.details(), exceptionThrown.getMessage());
   }

   @Test
   public void create_badRequestParameters_returns400Response() {
      final String account = "bad-account-request";
      final String origin = "bad-origin-request";
      final String freshdeskDomain = "my-freshworks";

      final IllegalArgumentException exceptionThrown = new IllegalArgumentException(
            "test-message");
      when(_taskService.create(account, origin, freshdeskDomain)).thenThrow(
            exceptionThrown);

      final ResponseEntity<?> response = _tasksController.create(
            new TasksController.CreateContactRequestBody(account, origin,
                  freshdeskDomain));

      assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
      final ErrorResponse body = (ErrorResponse) response.getBody();
      assertNotNull(body);
      assertEquals(body.message(), "Bad request parameters.");
      assertEquals(body.details(), exceptionThrown.getMessage());
   }

   @Test
   public void create_unknownException_returnsGeneralErrorMessage() {
      final String account = "bad-account-request";
      final String origin = "bad-origin-request";
      final String freshdeskDomain = "my-freshworks";

      final RuntimeException exception = new RuntimeException();
      when(_taskService.create(account, origin, freshdeskDomain)).thenThrow(
            exception);

      final ResponseEntity<?> response = _tasksController.create(
            new TasksController.CreateContactRequestBody(account, origin,
                  freshdeskDomain));

      assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
      final ErrorResponse body = (ErrorResponse) response.getBody();
      assertNotNull(body);
      assertEquals(body.message(), "Bad request parameters.");
      assertEquals(body.details(),
            "Something went wrong. Please contact the support team for further assistance.");
   }
}
