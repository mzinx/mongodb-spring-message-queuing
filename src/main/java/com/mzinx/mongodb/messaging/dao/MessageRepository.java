package com.mzinx.mongodb.messaging.dao;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.mzinx.mongodb.messaging.model.Message;



@Repository
public interface MessageRepository extends MongoRepository<Message, String>, CustomMessageRepository {


}
