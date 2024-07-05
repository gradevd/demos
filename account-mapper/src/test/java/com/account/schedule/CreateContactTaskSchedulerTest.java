package com.account.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.account.constants.Constants;
import com.account.entity.CreateContactTaskEntity;
import com.account.repository.CreateContactTaskRepository;
import com.account.service.CreateContactTaskService;

public class CreateContactTaskSchedulerTest {

   private CreateContactTaskScheduler _scheduler;

   @Mock
   private CreateContactTaskRepository _repository;
   @Mock
   private CreateContactTaskService _service;

   private int _maxAttempts = 5;
   private int initialTimeoutMillis = 5;
   private int timeoutStepMillis = 2;

   @BeforeEach
   public void beforeEach() {
      _scheduler = new CreateContactTaskScheduler(_repository, _service, 5, 5,
            2);
   }

   @Test
   public void test() {
      CreateContactTaskEntity task1 = new CreateContactTaskEntity();
      task1.status = Constants.CreateContactTaskStatus.TO_RETRY;
   }
}
