package com.account;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.account.constants.Constants;
import com.account.entity.CreateContactTaskEntity;
import com.account.repository.CreateContactTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An integration test that verifies the correct integration of the app's
 * components - web layer, db layer, service layer.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AccountMapperIntegrationTest {

   @Container
   private static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
         "mongo:latest");

   @Autowired
   private MockMvc _mockMvc;

   @Autowired
   private CreateContactTaskRepository _createContactTaskRepository;

   @BeforeEach
   public void setUp() {
      System.setProperty("spring.data.mongodb.uri",
            mongoDBContainer.getReplicaSetUrl());
   }

   @AfterEach
   public void cleanUp() {
      _createContactTaskRepository.deleteAll();
   }

   @Test
   public void createContacts_returnsCreatedTask() throws Exception {
      final RequestBuilder mockRequest = MockMvcRequestBuilders.post(
                  "/tasks").accept(MediaType.APPLICATION_JSON)
            .content("{\"account\":\"account\",\"origin\":\"github\", \"freshdeskDomain\":\"domain\"}")
            .contentType(MediaType.APPLICATION_JSON);
      final MvcResult result = _mockMvc.perform(mockRequest)
            .andExpect(status().isCreated()).andReturn();
      final CreateContactTaskEntity response = new ObjectMapper().readValue(
            result.getResponse().getContentAsString(),
            CreateContactTaskEntity.class);
      assertThat(response.accountOrigin).isEqualTo(
            Constants.AccountOrigin.GITHUB);
      assertThat(response.account).isEqualTo("account");
   }
}
