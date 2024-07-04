package com.account.error;

/**
 * An unchecked exception thrown in case of a duplicate task creation.
 */
public class DuplicateTaskException extends IllegalArgumentException {
   public DuplicateTaskException(final String message) {
      super(message);
   }
}
