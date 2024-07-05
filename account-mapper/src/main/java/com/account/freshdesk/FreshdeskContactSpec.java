package com.account.freshdesk;

import com.account.constants.Constants;
import com.account.github.GithubAccountInfo;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FreshdeskContactSpec {

   public String name;
   public String email;
   public String address;
   @JsonProperty("unique_external_id")
   public String uniqueExternalId;

   public static FreshdeskContactSpec from(
         final GithubAccountInfo gitHubAccountInfo) {
      final FreshdeskContactSpec spec = new FreshdeskContactSpec();
      spec.name = gitHubAccountInfo.name;
      spec.email = gitHubAccountInfo.email;
      spec.address = gitHubAccountInfo.location;
      spec.uniqueExternalId = String.format("%s:%s",
            Constants.AccountOrigin.GITHUB, gitHubAccountInfo.id);
      return spec;
   }

   @Override
   public String toString() {
      return String.format(
            "Name: %s; Email: %s; Address: %s; Unique External ID: %s'", name,
            email, address, uniqueExternalId);
   }
}
