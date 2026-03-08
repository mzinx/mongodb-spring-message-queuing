package com.mzinx.mongodb.messaging.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.mzinx.mongodb.messaging.config.MessagingProperties;
import com.mzinx.mongodb.messaging.model.Message;
import com.mzinx.mongodb.messaging.service.MessageService;



/**
 *
 * @param <T>
 */
@Controller
public class MessageController {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private MessageService messageService;
	
    @Autowired
    private MessagingProperties messagingProperties;
	
    @Autowired
	private SimpMessagingTemplate template;
	

	@MessageMapping("#{@messagingProperties.pushPath}")
	//@SendTo("/cmd") //no support to dynamic destinations, i.e. path from properties
	public Message push(Message message) throws Exception {
		message = messageService.queue(message);
		template.convertAndSend(messagingProperties.getCommandPath(), message);
		return message;
	}


}