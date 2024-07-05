package com.account.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GithubAccountInfo {
   public String id;
   @JsonProperty("login")
   public String name;
   public String location;
   public String email;

   @Override
   public String toString() {
      return "GitHubUserInfo{" + "id=" + id + ", name='" + name + '\''
            + ", location='" + location + '\'' + ", email='" + email + '\''
            + '}';
   }
}
