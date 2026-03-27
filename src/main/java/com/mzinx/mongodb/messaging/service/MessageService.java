package com.mzinx.mongodb.messaging.service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.changestream.OperationType;
import com.mzinx.mongodb.changestream.model.ChangeStream;
import com.mzinx.mongodb.changestream.model.ChangeStream.Mode;
import com.mzinx.mongodb.changestream.model.ChangeStreamRegistry;
import com.mzinx.mongodb.changestream.service.ChangeStreamService;
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
	@Autowired
	private MessagingProperties messagingProperties;
	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private ChangeStreamService<Document> changeStreamService;
	@Autowired
	private CodecRegistry pojoCodecRegistry;

	private ChangeStream<Document> cs;

	@PostConstruct
	private void init() {
		mongoTemplate.getCollection(messagingProperties.getCollection()).createIndex(Indexes.descending(INDEX_KEY),
				new IndexOptions().expireAfter(messagingProperties.getMaxLifeTime(), TimeUnit.MILLISECONDS)
						.name(INDEX_NAME));
		this.cs = ChangeStream.of("message-service", Mode.BOARDCAST,
				List.of());
		changeStreamService.start(
				ChangeStreamRegistry.<Document>builder().collectionName(messagingProperties.getCollection()).body(e -> {
					try {
						if (OperationType.INSERT == e.getOperationType()) {
							Document fullDoc = e.getFullDocument();
							Message message = pojoCodecRegistry.get(Message.class).decode(
									fullDoc.toBsonDocument().asBsonReader(),
									DecoderContext.builder().build());
							message.setType(Type.RES);
							if (message.getTarget() == null)
								message.setTarget(messagingProperties.getCommandPath());
							this.send(message);
						}
					} catch (Exception ex) {
						logger.error("Error sending message.", ex);
					}
				}).changeStream(this.cs).build());

	}

	@PreDestroy
	private void clear() {
		if (cs != null)
			cs.setRunning(false);
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