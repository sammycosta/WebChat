package com.ufsc.webchat.database.service;

import static java.util.Objects.isNull;

import org.json.JSONObject;

import com.ufsc.webchat.database.command.MessageListByChatIdQueryCommand;
import com.ufsc.webchat.database.command.MessageSaveCommand;
import com.ufsc.webchat.database.model.MessageCreateDto;
import com.ufsc.webchat.database.validator.AuthorizationValidator;
import com.ufsc.webchat.database.validator.MessageValidator;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;

public class MessageService {

	private final MessageSaveCommand messageSaveCommand = new MessageSaveCommand();
	private final MessageListByChatIdQueryCommand messageListByChatIdQueryCommand = new MessageListByChatIdQueryCommand();
	private final MessageValidator messageValidator = new MessageValidator();
	private final AuthorizationValidator authorizationValidator = new AuthorizationValidator();

	public ServiceResponse saveMessage(JSONObject payload) {
		var messageDto = new MessageCreateDto();
		messageDto.setMessage(payload.getString("message"));
		messageDto.setChatId(payload.getLong("chatId"));
		messageDto.setSenderId(payload.getLong("userId"));

		ValidationMessage validationMessage = this.messageValidator.validate(messageDto);

		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}

		Long messageId = this.messageSaveCommand.execute(messageDto);
		if (isNull(messageId)) {
			return new ServiceResponse(Status.ERROR, "Erro ao salvar mensagem!", null);
		}

		return new ServiceResponse(Status.CREATED, null, messageId);
	}

	public ServiceResponse loadMessages(JSONObject payload) {
		Long chatId = payload.getLong("chatId");
		Long userId = payload.getLong("userId");

		ValidationMessage validationMessage = this.authorizationValidator.validateUserInChat(chatId, userId);
		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}

		return new ServiceResponse(Status.OK, null, this.messageListByChatIdQueryCommand.execute(chatId));
	}

}
