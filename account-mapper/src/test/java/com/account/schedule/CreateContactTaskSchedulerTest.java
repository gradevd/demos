package com.account.schedule;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.account.constants.Constants;
import com.account.entity.CreateContactTaskEntity;
import com.account.repository.CreateContactTaskRepository;
import com.account.service.CreateContactTaskService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
public class CreateContactTaskSchedulerTest {

   @InjectMocks
   private CreateContactTaskScheduler _scheduler;
   @Mock
   private CreateContactTaskRepository _repository;
   @Mock
   private CreateContactTaskService _service;

   public CreateContactTaskSchedulerTest() {
      MockitoAnnotations.openMocks(this);
   }

   @BeforeEach
   public void setup() {
      ReflectionTestUtils.setField(_scheduler, "_maxAttempts", 5);
      ReflectionTestUtils.setField(_scheduler, "_initialTimeoutValueMillis",
            10000);
      ReflectionTestUtils.setField(_scheduler, "_timeoutStepMillis", 10000);
   }

   @Test
   public void scheduleTasks_newTasks_areImmediatelyScheduled() {
      final CreateContactTaskEntity task1 = task("1", 0, new Date());
      final CreateContactTaskEntity task2 = task("2", 0, new Date());
      final CreateContactTaskEntity task3 = task("3", 0, new Date());
      final CreateContactTaskEntity task4 = task("4", 0, new Date());
      when(_repository.findByStatusInOrderByUpdatedAsc(any())).thenReturn(
            List.of(task1, task2, task3, task4));
      _scheduler.scheduleTasks();
      // Verify all tasks were executed
      final ArgumentCaptor<String> taskIds = ArgumentCaptor.forClass(
            String.class);
      verify(_service, times(4)).execute(taskIds.capture());
      assertEquals(taskIds.getAllValues(),
            List.of(task1.id, task2.id, task3.id, task4.id));
   }

   @Test
   public void scheduleTasks_tasksWithExceededAttempts_areNotScheduled() {
      final CreateContactTaskEntity task1 = task("1", 5, new Date());
      final CreateContactTaskEntity task2 = task("2", 5, new Date());
      final CreateContactTaskEntity task3 = task("3", 5, new Date());
      final CreateContactTaskEntity task4 = task("4", 5, new Date());
      when(_repository.findByStatusInOrderByUpdatedAsc(any())).thenReturn(
            List.of(task1, task2, task3, task4));
      _scheduler.scheduleTasks();
      // Verify no tasks were executed
      final ArgumentCaptor<String> taskIds = ArgumentCaptor.forClass(
            String.class);
      verify(_service, times(0)).execute(taskIds.capture());
      assertTrue(taskIds.getAllValues().isEmpty());
   }

   @Test
   public void scheduleTasks_onlyTasksWithReachedTimeout_areExecuted() {
      final CreateContactTaskEntity task1 = task("1", 1,
            getLastUpdatedToPassTimeout(1));
      final CreateContactTaskEntity task2 = task("2", 1, new Date());
      final CreateContactTaskEntity task3 = task("3", 2,
            getLastUpdatedToPassTimeout(2));
      final CreateContactTaskEntity task4 = task("4", 3, new Date());
      final CreateContactTaskEntity task5 = task("5", 3,
            getLastUpdatedToPassTimeout(3));
      final CreateContactTaskEntity task6 = task("6", 4, new Date());
      final CreateContactTaskEntity task7 = task("7", 4,
            getLastUpdatedToPassTimeout(4));
      final CreateContactTaskEntity task8 = task("8", 4, new Date());
      when(_repository.findByStatusInOrderByUpdatedAsc(any())).thenReturn(
            List.of(task1, task2, task3, task4, task5, task6, task7, task8));
      _scheduler.scheduleTasks();
      // Verify the correct tasks were executed
      final ArgumentCaptor<String> taskIds = ArgumentCaptor.forClass(
            String.class);
      verify(_service, times(4)).execute(taskIds.capture());
      assertEquals(taskIds.getAllValues().size(), 4);
      assertEquals(taskIds.getAllValues(), List.of("1", "3", "5", "7"));
   }

   @Test
   public void cleanup_cleansUpAllReturnedValues() {
      final List<CreateContactTaskEntity> tasks = List.of(
            task("1", 4, new Date()), task("2", 4, new Date()),
            task("3", 4, new Date()), task("4", 4, new Date()));
      when(_repository.findByStatusInOrAttemptsGreaterThanEqual(
            List.of(Constants.CreateContactTaskStatus.FAILED,
                  Constants.CreateContactTaskStatus.COMPLETED), 5)).thenReturn(
            tasks);
      _scheduler.cleanupTasks();
      // Verify the correct tasks were executed
      Class<List<CreateContactTaskEntity>> listClass = (Class<List<CreateContactTaskEntity>>) (Class) List.class;
      final ArgumentCaptor<List<CreateContactTaskEntity>> capturedTasks = ArgumentCaptor.forClass(
            listClass);
      verify(_repository, times(1)).deleteAll(capturedTasks.capture());
      assertEquals(capturedTasks.getAllValues().size(), 1);
      assertEquals(capturedTasks.getAllValues().get(0), tasks);
   }

   private CreateContactTaskEntity task(final String id, final int attempts,
         final Date updated) {
      final CreateContactTaskEntity task = new CreateContactTaskEntity();
      task.id = id;
      task.attempts = attempts;
      task.updated = updated;
      return task;
   }

   private Date getLastUpdatedToPassTimeout(int attempt) {
      return new Date(new Date().getTime() - (10000/*initial*/
            + attempt/*currentAttempt*/ * 10000L/*step*/));
   }
}
