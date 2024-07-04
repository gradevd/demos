package com.account.controller;

/**
 * A simple POJO representing an error response returned by the REST controllers.
 * </p>
 * Used to standardize the structure of error responses sent to the client
 * in case of exceptions or errors during request processing.
 */
public class ErrorResponse {
   public final String message;
   public final String details;

   public ErrorResponse(final String message, final String details) {
      this.message = message;
      this.details = details;
   }
}
