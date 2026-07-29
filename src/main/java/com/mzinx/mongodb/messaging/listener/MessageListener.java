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
public class MessageListener implements ChangeStreamListener<Document> {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final CodecRegistry pojoCodecRegistry;
	private final MessageService messageService;
	private final MessagingProperties messagingProperties;

	MessageListener(CodecRegistry pojoCodecRegistry, MessageService messageService,
			MessagingProperties messagingProperties) {
		this.pojoCodecRegistry = pojoCodecRegistry;
		this.messageService = messageService;
		this.messagingProperties = messagingProperties;
	}

	@Override
    public void onEvent(ChangeStreamDocument<Document> event) {
		try {
			if (OperationType.INSERT == event.getOperationType()) {
				Document fullDoc = event.getFullDocument();
				Message message = pojoCodecRegistry.get(Message.class).decode(
						fullDoc.toBsonDocument().asBsonReader(),
						DecoderContext.builder().build());
				if (message.getTarget() == null)
					message.setTarget(messagingProperties.getCommandPath());
				this.messageService.broadcast(message);
			}
		} catch (Exception ex) {
			logger.error("Error sending message.", ex);
		}

	}
}
