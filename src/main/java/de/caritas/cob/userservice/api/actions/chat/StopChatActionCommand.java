package de.caritas.cob.userservice.api.actions.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

import de.caritas.cob.userservice.api.actions.ActionCommand;
import de.caritas.cob.userservice.api.adapters.rocketchat.RocketChatService;
import de.caritas.cob.userservice.api.exception.httpresponses.ConflictException;
import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.model.Chat;
import de.caritas.cob.userservice.api.service.ChatService;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Action to perform all necessary steps to stop an active group chat.
 */
@Component
public class StopChatActionCommand extends RecreateChatCapability implements ActionCommand<Chat> {

  @Autowired
  public StopChatActionCommand(@NonNull ChatService chatService,
      @NonNull RocketChatService rocketChatService) {
    super(chatService, rocketChatService, null, null, null);
  }

  /**
   * Deletes the given active chat and recreates it if repetitive.
   *
   * @param chat the {@link Chat} to be stopped
   */
  @Override
  public void execute(Chat chat) {
    checkActiveState(chat);

    if (isNull(chat.getGroupId())) {
      throw new InternalServerErrorException(
          String.format("Chat with id %s has no Rocket.Chat group id", chat.getId()));
    }

    if (!chat.isRepetitive() || nonNull(chat.nextStart())) {
      deleteChatGroup(chat);
      if (chat.isRepetitive()) {
        var rcGroupId = recreateMessengerChat(chat);
        recreateChat(chat, rcGroupId);
      }
    }
  }

  private void checkActiveState(Chat chat) {
    if (isFalse(chat.isActive())) {
      throw new ConflictException(
          String.format("Chat with id %s is already stopped.", chat.getId()));
    }
  }

  private void deleteChatGroup(Chat chat) {
    if (!rocketChatService.deleteGroupAsSystemUser(chat.getGroupId())) {
      throw new InternalServerErrorException(
          String.format("Could not delete Rocket.Chat group with id %s", chat.getGroupId()));
    }
    chatService.deleteChat(chat);
  }
}
