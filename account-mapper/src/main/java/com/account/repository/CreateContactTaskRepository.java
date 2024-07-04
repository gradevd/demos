package com.account.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.account.constants.Constants;
import com.account.entity.CreateContactTaskEntity;

@Repository
public interface CreateContactTaskRepository
      extends MongoRepository<CreateContactTaskEntity, String> {
   Optional<CreateContactTaskEntity> findByAccountAndAccountOrigin(
         final String account, final Constants.AccountOrigin accountOrigin);

}
