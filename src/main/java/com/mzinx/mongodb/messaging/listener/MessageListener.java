package com.mzinx.mongodb.messaging.listener;

import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import com.mzinx.mongodb.changestream.listener.ChangeStreamListener;
import com.mzinx.mongodb.messaging.config.MessagingProperties;
import com.mzinx.mongodb.messaging.model.Message;
import com.mzinx.mongodb.messaging.service.MessageService;

@Component
public class MessageListener<T> implements ChangeStreamListener<Document> {
	Logger logger = LoggerFactory.getLogger(getClass());

	private final CodecRegistry pojoCodecRegistry;
	private final MessageService messageService;
	private final MessagingProperties messagingProperties;

	MessageListener(CodecRegistry pojoCodecRegistry, MessageService messageService,
			MessagingProperties messagingProperties) {
		this.pojoCodecRegistry = pojoCodecRegistry;
		this.messageService = messageService;
		this.messagingProperties = messagingProperties;
	}

	public void execute(ChangeStreamDocument<Document> e) {
		try {
			if (OperationType.INSERT == e.getOperationType()) {
				Document fullDoc = e.getFullDocument();
				Message message = pojoCodecRegistry.get(Message.class).decode(
						fullDoc.toBsonDocument().asBsonReader(),
						DecoderContext.builder().build());
				if (message.getTarget() == null)
					message.setTarget(messagingProperties.getCommandPath());
				this.messageService.send(message);
			}
		} catch (Exception ex) {
			logger.error("Error sending message.", ex);
		}

	}
}
