package com.account.freshdesk;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FreshdeskContactInfo {
   public Long id;
   public String name;
   public String email;
   @JsonProperty("unique_external_id")
   public String uniqueExternalId;
   public String address;

   @Override
   public String toString() {
      return "FreshdeskContactInfo{" + "id=" + id + ", name='" + name + '\''
            + ", email='" + email + '\'' + ", uniqueExternalId='"
            + uniqueExternalId + '\'' + ", address='" + address + '\'' + '}';
   }
}
