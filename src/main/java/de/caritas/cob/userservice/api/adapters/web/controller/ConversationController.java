package de.caritas.cob.userservice.api.adapters.web.controller;

import static de.caritas.cob.userservice.api.conversation.model.ConversationListType.ARCHIVED_SESSION;
import static de.caritas.cob.userservice.api.conversation.model.ConversationListType.ARCHIVED_TEAM_SESSION;
import static de.caritas.cob.userservice.api.conversation.model.ConversationListType.REGISTERED_ENQUIRY;

import de.caritas.cob.userservice.api.adapters.web.dto.ConsultantSessionListResponseDTO;
import de.caritas.cob.userservice.api.adapters.web.mapping.ConversationDtoMapper;
import de.caritas.cob.userservice.api.conversation.service.ConversationListResolver;
import de.caritas.cob.userservice.api.helper.AuthenticatedUser;
import de.caritas.cob.userservice.api.port.in.Messaging;
import de.caritas.cob.userservice.generated.api.conversation.controller.ConversationsApi;
import io.swagger.annotations.Api;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Controller for conversation API requests. */
@RestController
@RequiredArgsConstructor
@Api(tags = "conversation-controller")
public class ConversationController implements ConversationsApi {

  private final @NonNull ConversationListResolver conversationListResolver;
  private final ConversationDtoMapper mapper;
  private final Messaging messenger;
  private final AuthenticatedUser authenticatedUser;

  /**
   * Entry point to retrieve all registered enquiries for current authenticated consultant.
   *
   * @param offset Number of items where to start in the query (0 = first item) (required)
   * @param count Number of items which are being returned (required)
   * @return the {@link ConsultantSessionListResponseDTO}
   */
  @Override
  public ResponseEntity<ConsultantSessionListResponseDTO> getRegisteredEnquiries(
      Integer offset, Integer count, @RequestHeader String rcToken) {

    ConsultantSessionListResponseDTO registeredEnquirySessions =
        this.conversationListResolver.resolveConversations(
            offset, count, REGISTERED_ENQUIRY, rcToken);

    return ResponseEntity.ok(registeredEnquirySessions);
  }

  /**
   * Entry point to retrieve all archived sessions for current authenticated consultant.
   *
   * @param offset Number of items where to start in the query (0 = first item) (required)
   * @param count Number of items which are being returned (required)
   * @return the {@link ConsultantSessionListResponseDTO}
   */
  @Override
  public ResponseEntity<ConsultantSessionListResponseDTO> getArchivedSessions(
      Integer offset, Integer count, @RequestHeader String rcToken) {

    ConsultantSessionListResponseDTO archivedSessions =
        this.conversationListResolver.resolveConversations(
            offset, count, ARCHIVED_SESSION, rcToken);

    return ResponseEntity.ok(archivedSessions);
  }

  /**
   * Entry point to retrieve all archived team sessions for current authenticated consultant.
   *
   * @param offset Number of items where to start in the query (0 = first item) (required)
   * @param count Number of items which are being returned (required)
   * @return the {@link ConsultantSessionListResponseDTO}
   */
  @Override
  public ResponseEntity<ConsultantSessionListResponseDTO> getArchivedTeamSessions(
      Integer offset, Integer count, @RequestHeader String rcToken) {

    ConsultantSessionListResponseDTO archivedTeamSessions =
        this.conversationListResolver.resolveConversations(
            offset, count, ARCHIVED_TEAM_SESSION, rcToken);

    return ResponseEntity.ok(archivedTeamSessions);
  }
}
