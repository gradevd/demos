package com.account.constants;

public class Constants {

   /**
    * Supported account origins.
    */
   public enum AccountOrigin {
      GITHUB
   }

   /**
    * Status of the contact creation task.
    */
   public enum CreateContactTaskStatus {
      NOT_STARTED, TO_RETRY, RUNNING, FAILED, COMPLETED
   }
}
