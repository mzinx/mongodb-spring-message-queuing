package com.mzinx.mongodb.messaging.service;


import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mzinx.mongodb.changestream.model.ChangeStream;
import com.mzinx.mongodb.messaging.config.MessagingProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class LiveDataService {
	Logger logger = LoggerFactory.getLogger(getClass());

	private final MessagingProperties messagingProperties;

	private ChangeStream<Document> cs;

	LiveDataService(MessagingProperties messagingProperties) {
		this.messagingProperties = messagingProperties;
	}

	@PostConstruct
	private void init() {
		// Live data 
		
		if (messagingProperties.getWatchCollections() != null
				&& messagingProperties.getWatchCollections().size() > 0) {
						//TODO: inject to change stream config instead of self init
			}
	}

	@PreDestroy
	private void destroy() {
		if (cs != null)
			cs.setRunning(false);
	}

}
