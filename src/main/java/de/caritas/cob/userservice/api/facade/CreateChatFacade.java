package de.caritas.cob.userservice.api.facade;

import static java.util.Objects.nonNull;

import de.caritas.cob.userservice.api.adapters.rocketchat.RocketChatService;
import de.caritas.cob.userservice.api.adapters.rocketchat.dto.group.GroupResponseDTO;
import de.caritas.cob.userservice.api.adapters.web.dto.ChatDTO;
import de.caritas.cob.userservice.api.adapters.web.dto.CreateChatResponseDTO;
import de.caritas.cob.userservice.api.exception.httpresponses.BadRequestException;
import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatAddUserToGroupException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatCreateGroupException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatUserNotInitializedException;
import de.caritas.cob.userservice.api.helper.RocketChatRoomNameGenerator;
import de.caritas.cob.userservice.api.model.Chat;
import de.caritas.cob.userservice.api.model.ChatAgency;
import de.caritas.cob.userservice.api.model.Consultant;
import de.caritas.cob.userservice.api.model.ConsultantAgency;
import de.caritas.cob.userservice.api.service.ChatService;
import de.caritas.cob.userservice.api.service.agency.AgencyService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Facade to encapsulate the steps for creating a chat. */
@Service
@RequiredArgsConstructor
public class CreateChatFacade {

  private final @NonNull ChatService chatService;
  private final @NonNull RocketChatService rocketChatService;
  private final @NonNull AgencyService agencyService;
  private final @NonNull ChatConverter chatConverter;

  /**
   * Creates a chat in MariaDB and Rocket.Chat-room and do not save any chat_agency relation.
   *
   * @param chatDTO {@link ChatDTO}
   * @param consultant {@link Consultant}
   * @return the generated chat link URL (String)
   */
  public CreateChatResponseDTO createChatV2(ChatDTO chatDTO, Consultant consultant) {
    Chat chat = null;
    String rcGroupId = null;

    try {
      chat = saveChatV2(consultant, chatDTO);
      rcGroupId = createRocketChatGroupWithTechnicalUser(chatDTO, chat);
      chat.setGroupId(rcGroupId);
      chatService.saveChat(chat);

      return new CreateChatResponseDTO()
          .groupId(rcGroupId)
          .createdAt(chat.getCreateDate().toString());
    } catch (InternalServerErrorException e) {
      doRollback(chat, rcGroupId);
      throw e;
    }
  }

  private Chat saveChatV2(Consultant consultant, ChatDTO chatDTO) {
    assertAgencyIdIsNotNull(chatDTO);
    Chat chat = chatService.saveChat(chatConverter.convertToEntity(chatDTO, consultant));
    ConsultantAgency foundConsultantAgency =
        findConsultantAgencyForGivenChatAgency(consultant, chatDTO);
    createChatAgencyRelation(chat, foundConsultantAgency.getAgencyId());
    return chat;
  }

  private void assertAgencyIdIsNotNull(ChatDTO chatDTO) {
    if (chatDTO.getAgencyId() == null) {
      throw new BadRequestException("Agency id must not be null");
    }
  }

  private ConsultantAgency findConsultantAgencyForGivenChatAgency(
      Consultant consultant, ChatDTO chatDTO) {
    return consultant.getConsultantAgencies().stream()
        .filter(agency -> agency.getAgencyId().equals(chatDTO.getAgencyId()))
        .findFirst()
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format(
                        "Consultant with id %s is not assigned to agency with id %s",
                        consultant.getId(), chatDTO.getAgencyId())));
  }

  private void createChatAgencyRelation(Chat chat, Long agencyId) {
    chatService.saveChatAgencyRelation(new ChatAgency(chat, agencyId));
  }

  private String createRocketChatGroupWithTechnicalUser(ChatDTO chatDTO, Chat chat) {
    String rcGroupId = createRocketChatGroup(chatDTO, chat);
    addTechnicalUserToGroup(chat, rcGroupId);
    return rcGroupId;
  }

  private void addTechnicalUserToGroup(Chat chat, String rcGroupId) {
    try {
      rocketChatService.addTechnicalUserToGroup(rcGroupId);
    } catch (RocketChatAddUserToGroupException e) {
      doRollback(chat, rcGroupId);
      throw new InternalServerErrorException(
          "Technical user could not be added to group chat " + "room");
    } catch (RocketChatUserNotInitializedException e) {
      doRollback(chat, rcGroupId);
      throw new InternalServerErrorException("Rocket chat user is not initialized");
    }
  }

  private String createRocketChatGroup(ChatDTO chatDTO, Chat chat) {
    try {
      GroupResponseDTO rcGroupDTO =
          rocketChatService
              .createPrivateGroupWithSystemUser(
                  new RocketChatRoomNameGenerator().generateGroupChatName(chat))
              .orElseThrow(
                  () ->
                      new RocketChatCreateGroupException(
                          "RocketChat group is not present while creating chat: " + chatDTO));
      return rcGroupDTO.getGroup().getId();
    } catch (RocketChatCreateGroupException e) {
      doRollback(chat, null);
      throw new InternalServerErrorException(
          "Error while creating private group in Rocket.Chat for group chat: "
              + chatDTO.toString());
    }
  }

  private void doRollback(Chat chat, String rcGroupId) {
    if (nonNull(chat)) {
      chatService.deleteChat(chat);
    }
    if (nonNull(rcGroupId)) {
      rocketChatService.deleteGroupAsSystemUser(rcGroupId);
    }
  }
}
