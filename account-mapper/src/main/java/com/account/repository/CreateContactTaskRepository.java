package com.account.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.account.constants.Constants;
import com.account.entity.CreateContactTaskEntity;

/**
 * A Spring Data interface providing some basic CRUD operations
 * on a repository of type {@link CreateContactTaskEntity}.
 * Additionally, the repository defines some more flexible interface method
 * following Spring Data's JPA Naming Convention.
 */
@Repository
public interface CreateContactTaskRepository
      extends MongoRepository<CreateContactTaskEntity, String> {
   List<CreateContactTaskEntity> findByAccountAndAccountOriginAndFreshdeskDomain(
         final String account, final Constants.AccountOrigin accountOrigin,
         final String freshdeskDomain);

   List<CreateContactTaskEntity> findByStatusInOrAttemptsGreaterThanEqual(
         List<Constants.CreateContactTaskStatus> statuses, int attemptsLimit);

   List<CreateContactTaskEntity> findByStatusInOrderByUpdatedAsc(
         List<Constants.CreateContactTaskStatus> statuses);
}
