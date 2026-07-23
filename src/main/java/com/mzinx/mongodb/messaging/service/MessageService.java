package com.mzinx.mongodb.messaging.service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mzinx.mongodb.changestream.model.ChangeStream.Mode;
import com.mzinx.mongodb.changestream.model.ChangeStreamConfig;
import com.mzinx.mongodb.changestream.service.ChangeStreamConfigService;
import com.mzinx.mongodb.messaging.config.MessagingProperties;
import com.mzinx.mongodb.messaging.dao.MessageRepository;
import com.mzinx.mongodb.messaging.model.Message;
import com.mzinx.mongodb.messaging.model.Message.Type;

import jakarta.annotation.PostConstruct;

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
	private final ChangeStreamConfigService changeStreamConfigService;

	MessageService(ChangeStreamConfigService changeStreamConfigService, MessagingProperties messagingProperties,
			SimpMessagingTemplate simpMessagingTemplate, MessageRepository messageRepository,
			MongoTemplate mongoTemplate) {
		this.changeStreamConfigService = changeStreamConfigService;
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
		changeStreamConfigService.save(ChangeStreamConfig.builder()
				.id("message-service") // unique change stream id
				.collectionName(messagingProperties.getCollection()) // collection to watch (null = whole database)
				.mode(Mode.BOARDCAST) // BOARDCAST, AUTO_RECOVER or AUTO_SCALE
				.pipeline(List.of())
				.listener("messageListener") // ChangeStreamListener bean name
				.enabled(true)
				.build());
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