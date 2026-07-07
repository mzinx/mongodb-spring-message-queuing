package com.mzinx.mongodb.messaging.service;

import java.util.List;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.UpdateDescription;
import com.mzinx.mongodb.changestream.model.ChangeStream;
import com.mzinx.mongodb.changestream.model.ChangeStreamRegistry;
import com.mzinx.mongodb.changestream.model.ChangeStream.Mode;
import com.mzinx.mongodb.changestream.service.ChangeStreamService;
import com.mzinx.mongodb.messaging.config.MessagingProperties;
import com.mzinx.mongodb.messaging.model.Message;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class LiveDataService {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private CodecRegistry pojoCodecRegistry;
	@Autowired
	private ChangeStreamService<Document> changeStreamService;
	@Autowired
	private MessageService messageService;
	@Autowired
	private MessagingProperties messagingProperties;

	private ChangeStream<Document> cs;

	@PostConstruct
	private void init() {
		// Live data change
		if (messagingProperties.getWatchCollections() != null
				&& messagingProperties.getWatchCollections().size() > 0) {
			this.cs = ChangeStream.of("live-data", Mode.BOARDCAST,
					List.of(Aggregates
							.match(Filters.in("ns.coll",
									messagingProperties.getWatchCollections()))));
			this.changeStreamService.start(ChangeStreamRegistry.<Document>builder().body(e -> {
				try {
					logger.info("{} operation on Document {} in collection {}, send refresh command",
							e.getOperationType().getValue(),
							e.getDocumentKey(),
							e.getNamespace().getCollectionName());
					switch (e.getOperationType()) {
						case INSERT:
						case REPLACE:
							this.messageService.send(Message.builder().type(Message.Type.RES).target(messagingProperties.getSyncPath())
									.content(e.getFullDocument())
									.build());
							break;
						case UPDATE:
							UpdateDescription updateDesc = e.getUpdateDescription();
							BsonDocument document = new BsonDocument();
							pojoCodecRegistry.get(UpdateDescription.class).encode(new BsonDocumentWriter(document),
									updateDesc, EncoderContext.builder().build());
							this.messageService.send(Message.builder().type(Message.Type.RES).target(messagingProperties.getSyncPath())
									.content(new Document(document))
									.build());
							break;
						case DELETE:
							this.messageService.send(Message.builder().type(Message.Type.RES).target(messagingProperties.getSyncPath())
									.content(new Document(e.getDocumentKey()))
									.build());
							break;
						default:
							break;
					}
					this.messageService.send(Message.builder().type(Message.Type.RES).target(messagingProperties.getCommandPath())
							.content(
									new Document("type", "REFRESH").append("coll",
											e.getNamespace().getCollectionName()))
							.build());
				} catch (Exception ex) {
					logger.error("Error publishing event.", ex);
				}
			}).changeStream(this.cs).build());
		}
	}

	@PreDestroy
	private void destroy() {
		if (cs != null)
			cs.setRunning(false);
	}

}
