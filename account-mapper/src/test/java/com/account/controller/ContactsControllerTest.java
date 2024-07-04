package com.account.controller;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ContactsControllerTest {

   @Autowired
   private MockMvc _mockMvc;

   @Test
   public void contacts_correctArguments_returns201() throws Exception {
      final RequestBuilder mockRequest = MockMvcRequestBuilders.post(
                  "/contacts").accept(MediaType.APPLICATION_JSON)
            .content("{\"account\":\"account\",\"origin\":\"github\"}")
            .contentType(MediaType.APPLICATION_JSON);
      final MvcResult result = _mockMvc.perform(mockRequest)
            .andExpect(status().isCreated()).andReturn();
      final Map<String, String> response = new ObjectMapper().readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<HashMap<String, String>>() {
            });
      assertThat(response.containsKey("key")).isTrue();
   }

   @Test
   public void contacts_missingAccount_returns400() throws Exception {
      final RequestBuilder mockRequest = MockMvcRequestBuilders.post(
                  "/contacts").accept(MediaType.APPLICATION_JSON)
            .content("{\"origin\":\"github\"}")
            .contentType(MediaType.APPLICATION_JSON);
      final MvcResult result = _mockMvc.perform(mockRequest)
            .andExpect(status().isBadRequest()).andReturn();
      final ErrorResponse response = new ObjectMapper().readValue(
            result.getResponse().getContentAsString(), ErrorResponse.class);
      assertThat(response.message).isNotBlank();
      assertThat(response.details).isNotBlank();
   }

   @Test
   public void contacts_missingOrigin_returns400() throws Exception {
      final RequestBuilder mockRequest = MockMvcRequestBuilders.post(
                  "/contacts").accept(MediaType.APPLICATION_JSON)
            .content("{\"account\":\"account\"}")
            .contentType(MediaType.APPLICATION_JSON);
      final MvcResult result = _mockMvc.perform(mockRequest)
            .andExpect(status().isBadRequest()).andReturn();
      final ErrorResponse response = new ObjectMapper().readValue(
            result.getResponse().getContentAsString(), ErrorResponse.class);
      assertThat(response.message).isNotBlank();
      assertThat(response.details).isNotBlank();
   }

   @Test
   public void contacts_wrongOrigin_returns400() throws Exception {
      final RequestBuilder mockRequest = MockMvcRequestBuilders.post(
                  "/contacts").accept(MediaType.APPLICATION_JSON)
            .content("{\"account\":\"account\",\"origin\":\"gitlab\"}")
            .contentType(MediaType.APPLICATION_JSON);
      final MvcResult result = _mockMvc.perform(mockRequest)
            .andExpect(status().isBadRequest()).andReturn();
      final ErrorResponse response = new ObjectMapper().readValue(
            result.getResponse().getContentAsString(), ErrorResponse.class);
      assertThat(response.message).isNotBlank();
      assertThat(response.details).isNotBlank();
   }
}
