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
import com.mzinx.mongodb.messaging.config.MessagingProperties;
import com.mzinx.mongodb.messaging.model.Message;
import com.mzinx.mongodb.messaging.service.MessageService;

@Component
public class LiveDataListener<T> implements ChangeStreamListener<Document> {
    Logger logger = LoggerFactory.getLogger(getClass());

	private final CodecRegistry pojoCodecRegistry;
	private final MessageService messageService;
	private final MessagingProperties messagingProperties;

	LiveDataListener(CodecRegistry pojoCodecRegistry, MessageService messageService, MessagingProperties messagingProperties) {
		this.pojoCodecRegistry = pojoCodecRegistry;
		this.messageService = messageService;
		this.messagingProperties = messagingProperties;
	}

    public void execute(ChangeStreamDocument<Document> e) {
				try {
					logger.info("{} operation on Document {} in collection {}, send refresh command",
							e.getOperationType().getValue(),
							e.getDocumentKey(),
							e.getNamespace().getCollectionName());
					switch (e.getOperationType()) {
						case INSERT:
						case REPLACE:
							this.messageService.send(Message.builder().target(messagingProperties.getSyncPath())
									.content(e.getFullDocument())
									.build());
							break;
						case UPDATE:
							UpdateDescription updateDesc = e.getUpdateDescription();
							BsonDocument document = new BsonDocument();
							pojoCodecRegistry.get(UpdateDescription.class).encode(new BsonDocumentWriter(document),
									updateDesc, EncoderContext.builder().build());
							this.messageService.send(Message.builder().target(messagingProperties.getSyncPath())
									.content(new Document(document))
									.build());
							break;
						case DELETE:
							this.messageService.send(Message.builder().target(messagingProperties.getSyncPath())
									.content(new Document(e.getDocumentKey()))
									.build());
							break;
						default:
							break;
					}
					this.messageService.send(Message.builder().target(messagingProperties.getCommandPath())
							.content(
									new Document("type", "REFRESH").append("coll",
											e.getNamespace().getCollectionName()))
							.build());
				} catch (Exception ex) {
					logger.error("Error publishing event.", ex);
				}
			
    }
}
