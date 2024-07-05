package com.account;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.account.interceptor.ApiAuthInterceptor;

@EnableAsync
@EnableMongoAuditing
@EnableScheduling
@SpringBootApplication
public class Application implements WebMvcConfigurer {
   /**
    * The entry point of the Spring Boot Application.
    * The Application can be run in two modes:
    * </p>
    * * Web Interface - The default behaviour of the application. A web server
    * is initialized on the specified ${server.port}.
    * </p>
    * * Command Line Interface - when the `--command.line.interface.enabled=true`
    * is passed, a Tomcat Web Server is not initialized and the program is run
    * as a standalone command line program instead.
    * </p>
    *
    * @param args - The program arguments.
    */
   public static void main(String[] args) {
      final SpringApplication app = new SpringApplication(Application.class);
      //@formatter:off
      final boolean cmdLineMode = Boolean.parseBoolean(Arrays.stream(args)
            .filter(arg -> arg.startsWith("--command.line.interface.enabled="))
            .findFirst()
            .orElse("--command.line.interface.enabled=false")
            .split("=")[1]
      );
      //@formatter:on
      app.setWebApplicationType(
            cmdLineMode ? WebApplicationType.NONE : WebApplicationType.SERVLET);
      app.run(args);
   }

   /**
    * A custom rest template bean with a custom interceptor for setting
    * API Authentication keys if needed.
    *
    * @param interceptor - The {@code ClientHttpRequestInterceptor}
    *                    implementation used to handle the HTTP requests.
    * @return An instance of the {@code RestTemplate} with the configured
    * interceptor.
    */
   @Bean
   public RestTemplate restTemplate(final ApiAuthInterceptor interceptor) {
      final RestTemplate restTemplate = new RestTemplate();
      restTemplate.setInterceptors(Collections.singletonList(interceptor));
      return restTemplate;
   }

   /**
    * A configurable {@code Executor} instance used to asynchronously execute
    * tasks.
    *
    * @param corePoolSize  The core number of threads used for task executions.
    * @param maxPoolSize   The max number of threads used for task execution.
    * @param queueCapacity The number of threads allowed to wait in the queue
    *                      for execution.
    * @return The configured {@code Executor} instance used for async task execution.
    */
   @Bean
   public Executor taskExecutor(
         @Value("${create.contact.task.executor.core.pool.size}") final int corePoolSize,
         @Value("${create.contact.task.executor.max.pool.size}") final int maxPoolSize,
         @Value("${create.contact.task.executor.queue.capacity}") final int queueCapacity) {
      final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(corePoolSize);
      executor.setMaxPoolSize(maxPoolSize);
      executor.setQueueCapacity(queueCapacity);
      executor.setThreadNamePrefix("AsyncTaskExecutor-");
      executor.initialize();
      return executor;
   }

   /**
    * A configurable {@code TaskScheduler} instance used to schedule tasks for
    * asynchronous execution.
    *
    * @param threadPoolSize           The number of threads used for task scheduling.
    * @param terminationTimeoutMillis The amount of time the scheduler will wait
    *                                 for ongoing tasks to complete on shutdown.
    * @return The configured {@code TaskScheduler} instance used for task
    * execution scheduling.
    */
   @Bean(name = "taskScheduler")
   public ThreadPoolTaskScheduler taskScheduler(
         @Value("${create.contact.task.scheduler.thread.pool.size}") final int threadPoolSize,
         @Value("${create.contact.task.scheduler.await.termination.timeout.millis}") final int terminationTimeoutMillis) {
      final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setPoolSize(threadPoolSize);
      scheduler.setThreadNamePrefix("TaskScheduler-");
      scheduler.setAwaitTerminationSeconds(terminationTimeoutMillis / 1000);
      scheduler.setWaitForTasksToCompleteOnShutdown(true);
      return scheduler;
   }

   /**
    * A configurable {@code TaskScheduler} instance used to schedule completed
    * tasks cleanup.
    *
    * @param threadPoolSize           The number of threads used for task scheduling.
    * @param terminationTimeoutMillis The amount of time the scheduler will wait
    *                                 for ongoing tasks to complete on shutdown.
    * @return The configured {@code TaskScheduler} instance used for scheduling
    * cleanup tasks.
    */
   @Bean(name = "taskCleaner")
   public ThreadPoolTaskScheduler taskCleaner(
         @Value("${create.contact.task.cleaner.thread.pool.size}") final int threadPoolSize,
         @Value("${create.contact.task.cleaner.await.termination.timeout.millis}") final int terminationTimeoutMillis) {
      final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setPoolSize(threadPoolSize);
      scheduler.setThreadNamePrefix("TaskCleaner-");
      scheduler.setAwaitTerminationSeconds(terminationTimeoutMillis / 1000);
      scheduler.setWaitForTasksToCompleteOnShutdown(true);
      return scheduler;
   }
}
