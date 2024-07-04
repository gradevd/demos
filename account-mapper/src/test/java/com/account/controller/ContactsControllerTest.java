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

public class ContactsControllerTest {

   @InjectMocks
   private ContactsController _contactsController;

   @Mock
   private CreateContactTaskService _taskService;

   public ContactsControllerTest() {
      MockitoAnnotations.openMocks(this);
   }

   @Test
   public void create_withCorrectArguments_returnsTheCreatedTask() {
      final String account = "test-account";
      final String origin = "github";
      final CreateContactTaskEntity expectedTaskEntity = new CreateContactTaskEntity(
            account, Constants.AccountOrigin.valueOf(origin.toUpperCase()));
      expectedTaskEntity.id = "1";

      when(_taskService.create(account, origin)).thenReturn(expectedTaskEntity);

      final ResponseEntity<?> response = _contactsController.create(
            new ContactsController.CreateContactRequestBody(account, origin));

      assertEquals(response.getStatusCode(), HttpStatus.CREATED);
      final CreateContactTaskEntity entity = (CreateContactTaskEntity) response.getBody();
      assertNotNull(entity);
      assertEquals(entity.id, expectedTaskEntity.id);
      assertEquals(entity.account, expectedTaskEntity.account);
      assertEquals(entity.accountOrigin, expectedTaskEntity.accountOrigin);
   }

   @Test
   public void create_withCorrectArguments_whenDuplicateTaskExists_returnsConflictResponse() {
      final String account = "test-account";
      final String origin = "github";

      final DuplicateTaskException exceptionThrown = new DuplicateTaskException(
            "test-message");
      when(_taskService.create(account, origin)).thenThrow(exceptionThrown);

      final ResponseEntity<?> response = _contactsController.create(
            new ContactsController.CreateContactRequestBody(account, origin));

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

      final IllegalArgumentException exceptionThrown = new IllegalArgumentException(
            "test-message");
      when(_taskService.create(account, origin)).thenThrow(exceptionThrown);

      final ResponseEntity<?> response = _contactsController.create(
            new ContactsController.CreateContactRequestBody(account, origin));

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

      final RuntimeException exception = new RuntimeException();
      when(_taskService.create(account, origin)).thenThrow(exception);

      final ResponseEntity<?> response = _contactsController.create(
            new ContactsController.CreateContactRequestBody(account, origin));

      assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
      final ErrorResponse body = (ErrorResponse) response.getBody();
      assertNotNull(body);
      assertEquals(body.message(), "Bad request parameters.");
      assertEquals(body.details(),
            "Something went wrong. Please contact the support team for further assistance.");
   }
}
