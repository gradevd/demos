package com.account.entity;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import com.account.constants.Constants;
import com.mongodb.lang.NonNull;

/**
 * Represents a task information used to create a Freshdesk contact stored in a
 * MongoDB collection.
 * </p>
 * The entity class is used to map the collection data to a Java object.
 */
@Document(collection = "createContactTask")
public class CreateContactTaskEntity {
   // Task specific info
   @Id
   public String id;
   @NonNull
   public Constants.CreateContactTaskStatus status;
   @CreatedDate
   public Date created;
   @LastModifiedDate
   public Date updated;
   public int attempts;

   // Account specific info
   @NonNull
   public Constants.AccountOrigin accountOrigin;
   @NonNull
   public String account;
   public String externalAccountId;
   public String email;
   public String address;

   public CreateContactTaskEntity(final String account,
         final Constants.AccountOrigin origin) {
      this.account = account;
      this.accountOrigin = origin;
      this.status = Constants.CreateContactTaskStatus.NOT_STARTED;
   }

   public CreateContactTaskEntity() {
   }

   public boolean hasNotCompleted() {
      return !Constants.CreateContactTaskStatus.FAILED.equals(status)
            && !Constants.CreateContactTaskStatus.COMPLETED.equals(status);
   }

   @Override
   public String toString() {
      return String.format("TaskID: %s; Account: %s @ %s; Status: %s; Last Modified: %s", id,
            account, accountOrigin, status, updated);
   }
}
