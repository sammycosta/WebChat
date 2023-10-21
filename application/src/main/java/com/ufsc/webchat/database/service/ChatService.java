package com.ufsc.webchat.database.service;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.ufsc.webchat.database.command.ChatMemberSaveCommand;
import com.ufsc.webchat.database.command.ChatSaveCommand;
import com.ufsc.webchat.database.model.UserSearchResultDto;
import com.ufsc.webchat.database.validator.ChatGroupAdditionValidator;
import com.ufsc.webchat.database.validator.ChatGroupValidator;
import com.ufsc.webchat.model.ServiceResponse;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;

public class ChatService {

	private final UserService userService = new UserService();
	private final ChatGroupValidator chatGroupValidator = new ChatGroupValidator();
	private final ChatGroupAdditionValidator chatGroupAdditionValidator = new ChatGroupAdditionValidator();
	private final ChatSaveCommand chatSaveCommand = new ChatSaveCommand();
	private final ChatMemberSaveCommand chatMemberSaveCommand = new ChatMemberSaveCommand();

	public ServiceResponse addToChatGroup(JSONObject payload) {
		Long userId = payload.getLong("userId");
		Long chatId = payload.getLong("chatId");
		String addedUserName = payload.getString("addedUserName");

		Long addedUserId = this.userService.loadUserIdByName(addedUserName);

		ValidationMessage validationMessage = this.chatGroupAdditionValidator.validate(chatId, addedUserId, userId);
		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}

		boolean success = this.chatMemberSaveCommand.execute(chatId, addedUserId);
		if (!success) {
			return new ServiceResponse(Status.ERROR, "Erro ao criar grupo!", null);
		}

		return new ServiceResponse(Status.OK, "Usuário adicionado ao grupo com sucesso!", null);
	}

	public ServiceResponse saveChatGroup(JSONObject payload) {
		// TODO: Avaliar possíveis exceções se não houver os campos no payload.
		//  Pode ser uma ideia criar um payloadValidator que avalia esses campos antes de passar pro service.
		String groupName = payload.getString("groupName");
		List<String> usernames = payload.getJSONArray("membersUsernames").toList()
				.stream()
				.map(Object::toString)
				.distinct()
				.collect(Collectors.toCollection(ArrayList::new));

		UserSearchResultDto userSearchResultDto = this.loadUsersIdFromUsernames(usernames);
		ValidationMessage validationMessage = this.chatGroupValidator.validate(userSearchResultDto);
		if (!validationMessage.isValid()) {
			return new ServiceResponse(Status.ERROR, validationMessage.message(), null);
		}

		List<Long> chatMembers = userSearchResultDto.getFoundUsersIds();
		Long userId = payload.getLong("userId");

		if (!chatMembers.contains(userId)) {
			chatMembers.add(userId);
		}

		Long chatId = this.chatSaveCommand.execute(groupName, true);

		for (Long memberId : chatMembers) {
			boolean success = this.chatMemberSaveCommand.execute(chatId, memberId);
			if (!success) {
				return new ServiceResponse(Status.ERROR, "Erro ao criar grupo!", null);
			}
		}

		return new ServiceResponse(Status.OK, "Grupo criado com sucesso!", chatId);
	}

	private UserSearchResultDto loadUsersIdFromUsernames(List<String> usernames) {
		List<String> notFoundUsers = new ArrayList<>();
		List<Long> foundUsersIds = new ArrayList<>();
		usernames.forEach(member -> {
			Long userId = this.userService.loadUserIdByName(member);
			if (isNull(userId)) {
				notFoundUsers.add(member);
			} else {
				foundUsersIds.add(userId);
			}
		});
		var userSearchResultDto = new UserSearchResultDto();
		userSearchResultDto.setFoundUsersIds(foundUsersIds);
		userSearchResultDto.setNotFoundUsers(notFoundUsers);
		return userSearchResultDto;
	}
}
