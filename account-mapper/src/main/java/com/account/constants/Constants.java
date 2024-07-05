package com.account.constants;

public class Constants {
   // Github //
   public static final String GITHUB_API_HOST = "api.github.com";
   public static final String GITHUB_API_URL = String.format("https://%s/",
         GITHUB_API_HOST);
   public static final String GITHUB_USERS_API_PATH_TEMPLATE = "users/";
   public static final String GITHUB_API_V3_HEADER = "application/vnd.github.v3+json";

   // Freshdesk //
   public static final String FRESHDESK_DOMAIN = ".freshdesk.com";
   private static final String FRESHDESK_API_HOST = "%s" + FRESHDESK_DOMAIN;
   public static final String FRESHDESK_API_URL_TEMPLATE = String.format(
         "https://%s/api/v2/", FRESHDESK_API_HOST);
   public static final String FRESHDESK_CONTACTS_PATH = "contacts/";

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
