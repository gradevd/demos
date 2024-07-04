package com.account.error;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Used to standardize the structure of error responses sent to the client
 * in case of exceptions or errors during request processing.
 */
public record ErrorResponse(String message, String details) {
   public ErrorResponse(@JsonProperty("message") final String message,
         @JsonProperty("details") final String details) {
      this.message = message;
      this.details = details;
   }
}
