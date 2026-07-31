package com.mzinx.mongodb.messaging.listener;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.UpdateDescription;
import com.mzinx.mongodb.changestream.listener.ChangeStreamListener;
import com.mzinx.mongodb.messaging.command.CommandMessages;
import com.mzinx.mongodb.messaging.config.MessagingProperties;
import com.mzinx.mongodb.messaging.model.Message;
import com.mzinx.mongodb.messaging.service.MessageService;

@Component
public class LiveDataListener implements ChangeStreamListener<Document> {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final CodecRegistry pojoCodecRegistry;
	private final MessageService messageService;
	private final MessagingProperties messagingProperties;
	private final CommandMessages commandMessages;

	LiveDataListener(CodecRegistry pojoCodecRegistry, MessageService messageService,
			MessagingProperties messagingProperties, CommandMessages commandMessages) {
		this.pojoCodecRegistry = pojoCodecRegistry;
		this.messageService = messageService;
		this.messagingProperties = messagingProperties;
		this.commandMessages = commandMessages;
	}

	@Override
	public void onEvent(ChangeStreamDocument<Document> event) {
		try {
			logger.info("{} operation on Document {} in collection {}, send refresh command",
					event.getOperationType().getValue(),
					event.getDocumentKey(),
					event.getNamespace().getCollectionName());
			Document content = new Document("op", event.getOperationType().getValue())
					.append("k", event.getDocumentKey().get("_id")).append("db", event.getNamespace().getDatabaseName())
					.append("coll", event.getNamespace().getCollectionName());
			switch (event.getOperationType()) {
				case INSERT:
				case REPLACE:
					this.messageService.broadcast(Message.builder().target(messagingProperties.getSyncPath())
							.content(content.append("doc", event.getFullDocument()))
							.build());
					break;
				case UPDATE:
					UpdateDescription updateDesc = event.getUpdateDescription();
					BsonDocument document = new BsonDocument();
					pojoCodecRegistry.get(UpdateDescription.class).encode(new BsonDocumentWriter(document),
							updateDesc, EncoderContext.builder().build());
					this.messageService.broadcast(Message.builder().target(messagingProperties.getSyncPath())
							.content(content.append("changes", document))
							.build());
					break;
				case DELETE:
					this.messageService.broadcast(Message.builder().target(messagingProperties.getSyncPath())
							.content(content)
							.build());
					break;
				default:
					break;
			}
		} catch (Exception ex) {
			logger.error("Error publishing event.", ex);
		}

	}
}
