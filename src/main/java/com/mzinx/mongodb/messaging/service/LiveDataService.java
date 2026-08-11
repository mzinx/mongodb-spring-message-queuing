package com.mzinx.mongodb.messaging.service;

import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mzinx.mongodb.changestream.model.ChangeStream.Mode;
import com.mzinx.mongodb.changestream.model.ChangeStreamConfig;
import com.mzinx.mongodb.changestream.service.ChangeStreamConfigService;
import com.mzinx.mongodb.messaging.config.MessagingProperties;
import jakarta.annotation.PostConstruct;

@Service
public class LiveDataService {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final MessagingProperties messagingProperties;

	private final ChangeStreamConfigService changeStreamConfigService;

	LiveDataService(MessagingProperties messagingProperties, ChangeStreamConfigService changeStreamConfigService) {
		this.messagingProperties = messagingProperties;
		this.changeStreamConfigService = changeStreamConfigService;
	}

	@PostConstruct
	private void init() {
		// Live data

		if (messagingProperties.getWatchCollections() != null
				&& messagingProperties.getWatchCollections().size() > 0) {
			changeStreamConfigService.save(ChangeStreamConfig.builder()
					.id("live-data") // unique change stream id
					.mode(Mode.BROADCAST) // BROADCAST, AUTO_RECOVER or AUTO_SCALE					
                	.pipeline(List.of(new Document("$match", new Document("ns.coll",new Document("$in", messagingProperties.getWatchCollections())))))
					.listener("liveDataListener") // ChangeStreamListener bean name
					// WebSocket-bound: the liveDataListener bean + broker live in the
					// business app, so this stream always runs there.
					.runOn(ChangeStreamConfig.RunOn.BUSINESS)
					.enabled(true)
					.build());
		}
	}

}
