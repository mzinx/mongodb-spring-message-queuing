package com.mzinx.mongodb.messaging.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

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
	

	//@MessageMapping("#{@messagingProperties.pushPath}")
	//@SendTo("#{@messagingProperties.commandPath}")
	@MessageMapping("${messaging.push-path:/push}")
	@SendTo("${messaging.command-path:/cmd}")
	public Message push(Message message) throws Exception {
		return messageService.queue(message);
	}


}