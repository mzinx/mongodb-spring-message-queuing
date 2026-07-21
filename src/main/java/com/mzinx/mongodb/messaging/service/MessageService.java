package com.mzinx.mongodb.messaging.service;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import com.mzinx.mongodb.messaging.config.MessagingProperties;
import com.mzinx.mongodb.messaging.dao.MessageRepository;
import com.mzinx.mongodb.messaging.model.Message;
import com.mzinx.mongodb.messaging.model.Message.Type;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 *
 * @param <T>
 */
@Controller
public class MessageService {
	private static final String INDEX_KEY = "cAt";
	private static final String INDEX_NAME = "ttl";
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final MessagingProperties messagingProperties;
	private final SimpMessagingTemplate simpMessagingTemplate;

	private final MessageRepository messageRepository;

	private final MongoTemplate mongoTemplate;

	MessageService(MessagingProperties messagingProperties, SimpMessagingTemplate simpMessagingTemplate, MessageRepository messageRepository, MongoTemplate mongoTemplate) {
		this.messagingProperties = messagingProperties;
		this.simpMessagingTemplate = simpMessagingTemplate;
		this.messageRepository = messageRepository;
		this.mongoTemplate = mongoTemplate;
	}

	@PostConstruct
	private void init() {
		mongoTemplate.getCollection(messagingProperties.getCollection()).createIndex(Indexes.descending(INDEX_KEY),
				new IndexOptions().expireAfter(messagingProperties.getMaxLifeTime(), TimeUnit.MILLISECONDS)
						.name(INDEX_NAME));
						//TODO: inject to change stream config instead of self init
	}

	public Message queue(Message message) {
		logger.info("Boarcast message received, append to the queue");
		Date now = new Date();
		message.setCreatedAt(now);
		message.setType(null);
		messageRepository.save(message);
		message.setType(Type.ACK);
		return message;
	}

	public void send(Message message) {
		this.simpMessagingTemplate.convertAndSend(message.getTarget(),
				message);
	}

}