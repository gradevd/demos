package com.account.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.account.constants.Constants;

@RestController
@RequestMapping("contacts")
public class ContactsController {

   private final Logger _logger = LoggerFactory.getLogger(getClass());

   @PostMapping
   public ResponseEntity<?> create(
         @RequestBody final CreateContactRequestBody requestBody) {
      if (requestBody.account == null || requestBody.account.isEmpty()) {
         return missingAccountResponse();
      }
      final String account = requestBody.account;

      if (requestBody.origin == null || requestBody.origin.isEmpty()) {
         return missingAccountOriginResponse();
      }
      final Constants.FreshdeskContactExternalAccountOrigin origin;
      try {
         origin = Constants.FreshdeskContactExternalAccountOrigin.valueOf(
               requestBody.origin.toUpperCase());
      } catch (final IllegalArgumentException e) {
         return unsupportedAccountOriginResponse();
      }

      _logger.debug(
            "Received a request to create a Freshdesk contact for account {}@{}.",
            account, origin);
      final Map<String, String> testResponse = Map.of("key", "value");
      return ResponseEntity.status(HttpStatus.CREATED).body(testResponse);
   }

   private ResponseEntity<ErrorResponse> missingAccountResponse() {
      this._logger.error("Bad request parameter. Missing 'account' argument.");
      return ResponseEntity.badRequest()
            .body(new ErrorResponse("Bad request parameters.",
                  "Null, or empty account provided."));
   }

   private ResponseEntity<ErrorResponse> missingAccountOriginResponse() {
      this._logger.error(
            "Bad request parameter. Missing 'accountOrigin' argument.");
      return ResponseEntity.badRequest()
            .body(new ErrorResponse("Bad request parameters.",
                  "Null, or empty account origin provided."));
   }

   private ResponseEntity<ErrorResponse> unsupportedAccountOriginResponse() {
      final String details = String.format(
            "Bad request parameter. Account origin value must be one of %s.",
            List.of(Constants.FreshdeskContactExternalAccountOrigin.values()));
      this._logger.error(details);
      return ResponseEntity.badRequest()
            .body(new ErrorResponse("Bad request parameters.", details));
   }

   private static class CreateContactRequestBody {
      public String account;
      public String origin;
   }
}
