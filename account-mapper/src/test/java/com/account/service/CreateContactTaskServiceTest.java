package com.account.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class CreateContactTaskServiceTest {

   @InjectMocks
   private CreateContactTaskService _service;
   @Mock
   private CreateContactTaskRepository _repository;
   @Mock
   private GitHubAccountService _gitHubAccountService;
   @Mock
   private FreshdeskContactService _freshdeskContactService;

   public CreateContactTaskServiceTest() {
      MockitoAnnotations.openMocks(this);
   }

   @Test
   public void create_nullAccount_throwsException() {
      final IllegalArgumentException expectedException = assertThrows(
            IllegalArgumentException.class,
            () -> _service.create(null, "origin", "domain"));
      assertEquals(expectedException.getMessage(),
            "Account name must not be null or empty.");
   }

   @Test
   public void create_emptyAccount_throwsException() {
      final IllegalArgumentException expectedException = assertThrows(
            IllegalArgumentException.class,
            () -> _service.create("", "origin", "domain"));
      assertEquals(expectedException.getMessage(),
            "Account name must not be null or empty.");
   }

   @Test
   public void create_nullAccountOrigin_throwsException() {
      final IllegalArgumentException expectedException = assertThrows(
            IllegalArgumentException.class,
            () -> _service.create("account", null, "domain"));
      assertEquals(expectedException.getMessage(),
            "The account origin must not be null or empty.");
   }

   @Test
   public void create_emptyAccountOrigin_throwsException() {
      final IllegalArgumentException expectedException = assertThrows(
            IllegalArgumentException.class,
            () -> _service.create("account", null, "domain"));
      assertEquals(expectedException.getMessage(),
            "The account origin must not be null or empty.");
   }

   @Test
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

   @Test
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

   @Test
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

   @Test
   public void execute_ifTaskDoesNotExists_skipsTaskExecution() {
      final String taskId = "task-id";
      when(_repository.findById(taskId)).thenReturn(Optional.empty());
      _service.execute(taskId);
      verify(_repository, Mockito.never()).save(
            any(CreateContactTaskEntity.class));
      verifyNoInteractions(_gitHubAccountService, _freshdeskContactService);
   }

   @Test
   public void execute_ifTaskIsAlreadyAssigned_skipsTaskExecution() {
      final String taskId = "task-id";
      final CreateContactTaskEntity assignedTask = new CreateContactTaskEntity();
      assignedTask.status = Constants.CreateContactTaskStatus.RUNNING;
      when(_repository.findById(taskId)).thenReturn(Optional.of(assignedTask));
      _service.execute(taskId);
      verify(_repository, Mockito.never()).save(
            any(CreateContactTaskEntity.class));
      verifyNoInteractions(_gitHubAccountService, _freshdeskContactService);
   }

   @Test
   public void execute_whenTaskIsAssigned_andNoFreshdeskContactExists_createsANewOne() {
      // Mock the task assignment
      final CreateContactTaskEntity task = mockTaskAssignment();

      // Mock the task update after assignment
      final CreateContactTaskEntity assignedTask = mockAssignedTaskUpdate(task);

      // Mock the GitHub account call
      final GithubAccountInfo githubAccountInfo = mockGithubApiCall(
            assignedTask);

      // Mock the task update
      mockTaskUpdateAfterGithubAccountApiCall(assignedTask, githubAccountInfo);

      // Mock contact existence
      mockCheckContactExistence(assignedTask, githubAccountInfo, null);

      // Mock contact create call
      _service.execute(task.id);
      verify(_freshdeskContactService, times(1)).create(
            eq(assignedTask.freshdeskDomain), any(FreshdeskContactSpec.class));
   }

   @Test
   public void execute_whenTaskIsAssigned_andAFreshdeskContactExists_updatesTheExistingOne() {
      // Mock the task assignment
      final CreateContactTaskEntity task = mockTaskAssignment();

      // Mock the task update after assignment
      final CreateContactTaskEntity assignedTask = mockAssignedTaskUpdate(task);

      // Mock the GitHub account call
      final GithubAccountInfo githubAccountInfo = mockGithubApiCall(
            assignedTask);

      // Mock the task update
      mockTaskUpdateAfterGithubAccountApiCall(assignedTask, githubAccountInfo);

      // Mock contact existence
      final FreshdeskContactInfo info = new FreshdeskContactInfo();
      info.id = 5L;
      mockCheckContactExistence(assignedTask, githubAccountInfo, info);

      // Verify a contact update call
      _service.execute(task.id);
      verify(_freshdeskContactService, times(1)).update(
            eq(assignedTask.freshdeskDomain), eq(info.id),
            any(FreshdeskContactSpec.class));
   }

   @Test
   public void execute_whenSomeApiFailsWith5xxError_throwsRecoverableException() {
      // Mock the task assignment
      final CreateContactTaskEntity task = mockTaskAssignment();

      // Mock the task update after assignment
      mockAssignedTaskUpdate(task);

      // Mock the GitHub account call
      final HttpStatusCodeException e = mock(HttpStatusCodeException.class);
      when(e.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY);
      mockGithubApiCallFailure(e);

      _service.execute(task.id);

      ArgumentCaptor<CreateContactTaskEntity> assignedTaskCaptor = ArgumentCaptor.forClass(
            CreateContactTaskEntity.class);
      verify(_repository, times(2)).save(assignedTaskCaptor.capture());

      assertNotNull(assignedTaskCaptor.getValue());
      assertEquals(assignedTaskCaptor.getValue().status,
            Constants.CreateContactTaskStatus.TO_RETRY);
   }

   @Test
   public void execute_whenSomeApiFailsWithAnotherError_throwsRecoverableException() {
      // Mock the task assignment
      final CreateContactTaskEntity task = mockTaskAssignment();

      // Mock the task update after assignment
      mockAssignedTaskUpdate(task);

      // Mock the GitHub account call
      final HttpStatusCodeException e = mock(HttpStatusCodeException.class);
      when(e.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
      mockGithubApiCallFailure(e);

      _service.execute(task.id);

      ArgumentCaptor<CreateContactTaskEntity> assignedTaskCaptor = ArgumentCaptor.forClass(
            CreateContactTaskEntity.class);
      verify(_repository, times(2)).save(assignedTaskCaptor.capture());

      assertNotNull(assignedTaskCaptor.getValue());
      assertEquals(assignedTaskCaptor.getValue().status,
            Constants.CreateContactTaskStatus.FAILED);
   }

   private CreateContactTaskEntity mockTaskAssignment() {
      final CreateContactTaskEntity task = new CreateContactTaskEntity();
      task.id = "task-id";
      task.status = Constants.CreateContactTaskStatus.NOT_STARTED;
      task.account = "account";
      task.accountOrigin = Constants.AccountOrigin.GITHUB;
      task.freshdeskDomain = "domain";
      when(_repository.findById("task-id")).thenReturn(Optional.of(task));
      return task;
   }

   private CreateContactTaskEntity mockAssignedTaskUpdate(
         final CreateContactTaskEntity task) {
      final CreateContactTaskEntity assignedTask = new CreateContactTaskEntity();
      assignedTask.id = task.id;
      assignedTask.account = task.account;
      assignedTask.accountOrigin = task.accountOrigin;
      assignedTask.freshdeskDomain = task.freshdeskDomain;
      assignedTask.status = Constants.CreateContactTaskStatus.RUNNING;
      assignedTask.attempts++;
      when(_repository.save(any(CreateContactTaskEntity.class))).thenReturn(
            assignedTask);
      return assignedTask;
   }

   private GithubAccountInfo mockGithubApiCall(
         final CreateContactTaskEntity assignedTask) {
      final GithubAccountInfo githubAccountInfo = new GithubAccountInfo();
      githubAccountInfo.id = "1";
      githubAccountInfo.name = "github-user";
      githubAccountInfo.email = "user@gmail.com";
      when(_gitHubAccountService.get(assignedTask.account)).thenReturn(
            githubAccountInfo);
      return githubAccountInfo;
   }

   private void mockGithubApiCallFailure(final HttpStatusCodeException e) {
      when(_gitHubAccountService.get(anyString())).thenThrow(e);
   }

   private void mockTaskUpdateAfterGithubAccountApiCall(
         final CreateContactTaskEntity assignedTask,
         final GithubAccountInfo githubAccountInfo) {
      assignedTask.externalAccountId = githubAccountInfo.id;
      assignedTask.address = githubAccountInfo.location;
      assignedTask.email = githubAccountInfo.email;
      when(_repository.save(any(CreateContactTaskEntity.class))).thenReturn(
            assignedTask);
   }

   private void mockCheckContactExistence(
         final CreateContactTaskEntity assignedTask,
         final GithubAccountInfo githubAccountInfo,
         final FreshdeskContactInfo result) {
      final FreshdeskContactSpec contactSpec = FreshdeskContactSpec.from(
            githubAccountInfo);
      final Optional<FreshdeskContactInfo> expectedResult =
            result == null ? Optional.empty() : Optional.of(result);
      when(_freshdeskContactService.findByExternalId(
            assignedTask.freshdeskDomain,
            contactSpec.uniqueExternalId)).thenReturn(expectedResult);
   }

}
