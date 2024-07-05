package com.account.service;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.account.constants.Constants;
import com.account.entity.CreateContactTaskEntity;
import com.account.error.DuplicateTaskException;
import com.account.repository.CreateContactTaskRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateContactTaskServiceTest {

   @InjectMocks
   private CreateContactTaskService _service;
   @Mock
   private CreateContactTaskRepository _repository;

   public CreateContactTaskServiceTest() {
      MockitoAnnotations.openMocks(this);
   }

   @Test()
   public void create_nullAccount_throwsException() {
      final IllegalArgumentException expectedException = assertThrows(
            IllegalArgumentException.class,
            () -> _service.create(null, "origin", "domain"));
      assertEquals(expectedException.getMessage(),
            "Account name must not be null or empty.");
   }

   @Test()
   public void create_emptyAccount_throwsException() {
      final IllegalArgumentException expectedException = assertThrows(
            IllegalArgumentException.class,
            () -> _service.create("", "origin", "domain"));
      assertEquals(expectedException.getMessage(),
            "Account name must not be null or empty.");
   }

   @Test()
   public void create_nullAccountOrigin_throwsException() {
      final IllegalArgumentException expectedException = assertThrows(
            IllegalArgumentException.class,
            () -> _service.create("account", null, "domain"));
      assertEquals(expectedException.getMessage(),
            "The account origin must not be null or empty.");
   }

   @Test()
   public void create_emptyAccountOrigin_throwsException() {
      final IllegalArgumentException expectedException = assertThrows(
            IllegalArgumentException.class,
            () -> _service.create("account", null, "domain"));
      assertEquals(expectedException.getMessage(),
            "The account origin must not be null or empty.");
   }

   @Test()
   public void create_duplicateTask_throwsException_whenTaskIsNotCompleted() {
      final String account = "duplicate-account";
      final String origin = "github";
      final String domain = "domain";
      // Mock a pending task is returned
      final CreateContactTaskEntity task = mock(CreateContactTaskEntity.class);
      when(task.hasNotCompleted()).thenReturn(true);

      when(_repository.findByAccountAndAccountOriginAndFreshdeskDomain(account,
            Constants.AccountOrigin.valueOf(origin.toUpperCase()),
            domain)).thenReturn(List.of(task));

      final DuplicateTaskException exceptionThrown = assertThrows(
            DuplicateTaskException.class,
            () -> _service.create(account, origin, domain));
      assertEquals(exceptionThrown.getMessage(),
            "A duplicate pending task already exists.");
   }

   @Test()
   public void create_duplicateTask_createsANewTask_whenTaskIsCompleted() {
      final String account = "duplicate-account";
      final String origin = "github";
      final String domain = "domain";
      // Mock a pending task is returned
      final CreateContactTaskEntity existingTask = mock(
            CreateContactTaskEntity.class);
      when(existingTask.hasNotCompleted()).thenReturn(false);

      when(_repository.findByAccountAndAccountOriginAndFreshdeskDomain(account,
            Constants.AccountOrigin.valueOf(origin.toUpperCase()),
            domain)).thenReturn(List.of(existingTask));

      final CreateContactTaskEntity expected = new CreateContactTaskEntity(
            account, Constants.AccountOrigin.valueOf(origin.toUpperCase()),
            domain);
      when(_repository.save(any(CreateContactTaskEntity.class))).thenReturn(
            expected);

      final CreateContactTaskEntity task = _service.create(account, origin,
            domain);
      assertSame(expected, task);
   }

   @Test()
   public void create_returnANewTask() {
      final String account = "duplicate-account";
      final String origin = "github";
      final String domain = "domain";
      when(_repository.findByAccountAndAccountOriginAndFreshdeskDomain(account,
            Constants.AccountOrigin.valueOf(origin.toUpperCase()),
            domain)).thenReturn(Collections.emptyList());

      final CreateContactTaskEntity expected = new CreateContactTaskEntity(
            account, Constants.AccountOrigin.valueOf(origin.toUpperCase()),
            domain);
      when(_repository.save(any(CreateContactTaskEntity.class))).thenReturn(
            expected);

      final CreateContactTaskEntity task = _service.create(account, origin,
            domain);
      assertSame(expected, task);
   }
}
