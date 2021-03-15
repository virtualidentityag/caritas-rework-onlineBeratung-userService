package de.caritas.cob.userservice.api.facade;

import static de.caritas.cob.userservice.testHelper.TestConstants.ACTIVE_CHAT;
import static de.caritas.cob.userservice.testHelper.TestConstants.CHAT_ID;
import static de.caritas.cob.userservice.testHelper.TestConstants.CONSULTANT;
import static de.caritas.cob.userservice.testHelper.TestConstants.CONSULTANT_ROLES;
import static de.caritas.cob.userservice.testHelper.TestConstants.USER_ROLES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.exception.httpresponses.ForbiddenException;
import de.caritas.cob.userservice.api.exception.httpresponses.NotFoundException;
import de.caritas.cob.userservice.api.helper.AuthenticatedUser;
import de.caritas.cob.userservice.api.helper.ChatHelper;
import de.caritas.cob.userservice.api.model.ChatInfoResponseDTO;
import de.caritas.cob.userservice.api.repository.user.User;
import de.caritas.cob.userservice.api.service.ChatService;
import de.caritas.cob.userservice.api.service.ConsultantService;
import de.caritas.cob.userservice.api.service.user.UserService;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class GetChatFacadeTest {

  @InjectMocks
  private GetChatFacade getChatFacade;
  @Mock
  private ChatService chatService;
  @Mock
  private AuthenticatedUser authenticatedUser;
  @Mock
  private ChatHelper chatHelper;
  @Mock
  private ConsultantService consultantService;
  @Mock
  private User user;
  @Mock
  private UserService userService;

  /**
   * Method: getChat
   */

  @Test
  public void getChat_Should_ThrowNotFoundException_WhenChatDoesNotExist() {

    when(chatService.getChat(CHAT_ID)).thenReturn(Optional.empty());

    try {
      getChatFacade.getChat(CHAT_ID, authenticatedUser);
      fail("Expected exception: NotFoundException");
    } catch (NotFoundException notFoundException) {
      assertTrue("Excepted NotFoundException thrown", true);
    }

    verify(chatService, times(1)).getChat(CHAT_ID);

  }

  @Test
  public void getChat_Should_ThrowRequestForbiddenException_WhenConsultantHasNoPermissionForChat() {

    when(chatService.getChat(CHAT_ID)).thenReturn(Optional.of(ACTIVE_CHAT));
    when(authenticatedUser.getRoles()).thenReturn(CONSULTANT_ROLES);
    when(chatHelper.isChatAgenciesContainConsultantAgency(ACTIVE_CHAT, CONSULTANT))
        .thenReturn(false);
    when(consultantService.getConsultantViaAuthenticatedUser(authenticatedUser))
        .thenReturn(Optional.of(CONSULTANT));

    try {
      getChatFacade.getChat(CHAT_ID, authenticatedUser);
      fail("Expected exception: RequestForbiddenException");
    } catch (ForbiddenException requestForbiddenException) {
      assertTrue("Excepted RequestForbiddenException thrown", true);
    }

    verify(chatService, times(1)).getChat(CHAT_ID);
    verify(authenticatedUser, times(1)).getRoles();
    verify(chatHelper, times(1)).isChatAgenciesContainConsultantAgency(ACTIVE_CHAT, CONSULTANT);
    verify(consultantService, times(1)).getConsultantViaAuthenticatedUser(authenticatedUser);

  }

  @Test
  public void getChat_Should_ThrowRequestForbiddenException_WhenUserHasNoPermissionForChat() {

    when(chatService.getChat(CHAT_ID)).thenReturn(Optional.of(ACTIVE_CHAT));
    when(authenticatedUser.getRoles()).thenReturn(USER_ROLES);
    when(chatHelper.isChatAgenciesContainUserAgency(ACTIVE_CHAT, user)).thenReturn(false);
    when(userService.getUserViaAuthenticatedUser(authenticatedUser)).thenReturn(Optional.of(user));

    try {
      getChatFacade.getChat(CHAT_ID, authenticatedUser);
      fail("Expected exception: RequestForbiddenException");
    } catch (ForbiddenException requestForbiddenException) {
      assertTrue("Excepted RequestForbiddenException thrown", true);
    }

    verify(chatService, times(1)).getChat(CHAT_ID);
    verify(authenticatedUser, times(1)).getRoles();
    verify(chatHelper, times(1)).isChatAgenciesContainUserAgency(ACTIVE_CHAT, user);
    verify(userService, times(1)).getUserViaAuthenticatedUser(authenticatedUser);

  }

  @Test
  public void getChat_Should_ReturnValidChatInfoResponseDTOForUser() {

    when(chatService.getChat(ACTIVE_CHAT.getId())).thenReturn(Optional.of(ACTIVE_CHAT));
    when(authenticatedUser.getRoles()).thenReturn(USER_ROLES);
    when(chatHelper.isChatAgenciesContainUserAgency(ACTIVE_CHAT, user)).thenReturn(true);
    when(userService.getUserViaAuthenticatedUser(authenticatedUser)).thenReturn(Optional.of(user));

    ChatInfoResponseDTO result = getChatFacade.getChat(ACTIVE_CHAT.getId(), authenticatedUser);

    assertTrue(result instanceof ChatInfoResponseDTO);
    assertEquals(ACTIVE_CHAT.getId(), result.getId());
    assertEquals(ACTIVE_CHAT.getGroupId(), result.getGroupId());
    assertEquals(true, result.getActive());

    verify(chatService, times(1)).getChat(ACTIVE_CHAT.getId());
    verify(authenticatedUser, times(1)).getRoles();
    verify(chatHelper, times(1)).isChatAgenciesContainUserAgency(ACTIVE_CHAT, user);
    verify(userService, times(1)).getUserViaAuthenticatedUser(authenticatedUser);

  }

  @Test
  public void getChat_Should_ReturnValidChatInfoResponseDTOForConsultant() {

    when(chatService.getChat(ACTIVE_CHAT.getId())).thenReturn(Optional.of(ACTIVE_CHAT));
    when(authenticatedUser.getRoles()).thenReturn(CONSULTANT_ROLES);
    when(chatHelper.isChatAgenciesContainConsultantAgency(ACTIVE_CHAT, CONSULTANT))
        .thenReturn(true);
    when(consultantService.getConsultantViaAuthenticatedUser(authenticatedUser))
        .thenReturn(Optional.of(CONSULTANT));

    ChatInfoResponseDTO result = getChatFacade.getChat(ACTIVE_CHAT.getId(), authenticatedUser);

    assertTrue(result instanceof ChatInfoResponseDTO);
    assertEquals(ACTIVE_CHAT.getId(), result.getId());
    assertEquals(ACTIVE_CHAT.getGroupId(), result.getGroupId());
    assertEquals(true, result.getActive());

    verify(chatService, times(1)).getChat(ACTIVE_CHAT.getId());
    verify(authenticatedUser, times(1)).getRoles();
    verify(chatHelper, times(1)).isChatAgenciesContainConsultantAgency(ACTIVE_CHAT, CONSULTANT);
    verify(consultantService, times(1)).getConsultantViaAuthenticatedUser(authenticatedUser);

  }

}
