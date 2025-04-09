package de.caritas.cob.userservice.api.adapters.web.controller;

import static de.caritas.cob.userservice.api.config.auth.Authority.AuthorityValue.USER_DEFAULT;
import static de.caritas.cob.userservice.api.conversation.model.ConversationListType.REGISTERED_ENQUIRY;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_TOKEN;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_TOKEN_HEADER_PARAMETER_NAME;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.caritas.cob.userservice.api.config.auth.Authority.AuthorityValue;
import de.caritas.cob.userservice.api.conversation.service.ConversationListResolver;
import de.caritas.cob.userservice.api.helper.UsernameTranscoder;
import de.caritas.cob.userservice.api.service.session.SessionTopicEnrichmentService;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("testing")
@SpringBootTest
@AutoConfigureMockMvc
class ConversationControllerAuthorizationIT {

  private final String CSRF_COOKIE = "csrfCookie";
  private final String CSRF_HEADER = "csrfHeader";
  private final String CSRF_VALUE = "test";
  private final Cookie csrfCookie = new Cookie(CSRF_COOKIE, CSRF_VALUE);

  @Autowired private MockMvc mvc;

  @MockBean private ConversationListResolver conversationListResolver;

  @MockBean private SessionTopicEnrichmentService sessionTopicEnrichmentService;

  @MockBean
  @SuppressWarnings("unused")
  private UsernameTranscoder usernameTranscoder;

  @Test
  @WithMockUser(authorities = {AuthorityValue.CONSULTANT_DEFAULT})
  void getRegisteredEnquiries_Should_ReturnOK_When_ProperlyAuthorizedWithConsultantAuthority()
      throws Exception {
    this.mvc
        .perform(
            get(ConversationControllerIT.GET_REGISTERED_ENQUIRIES_PATH)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header(RC_TOKEN_HEADER_PARAMETER_NAME, RC_TOKEN)
                .param("offset", "0")
                .param("count", "10")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    verify(this.conversationListResolver, times(1))
        .resolveConversations(0, 10, REGISTERED_ENQUIRY, RC_TOKEN);
  }

  @Test
  void
      getRegisteredEnquiries_Should_ReturnUnauthorizedAndCallNoMethods_When_NoKeycloakAuthorization()
          throws Exception {
    this.mvc
        .perform(
            get(ConversationControllerIT.GET_REGISTERED_ENQUIRIES_PATH)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(conversationListResolver);
  }

  @Test
  @WithMockUser(
      authorities = {
        AuthorityValue.ASSIGN_CONSULTANT_TO_SESSION,
        AuthorityValue.ASSIGN_CONSULTANT_TO_ENQUIRY,
        AuthorityValue.TECHNICAL_DEFAULT,
        AuthorityValue.VIEW_AGENCY_CONSULTANTS,
        AuthorityValue.CREATE_NEW_CHAT,
        AuthorityValue.START_CHAT,
        AuthorityValue.STOP_CHAT,
        AuthorityValue.ASSIGN_CONSULTANT_TO_SESSION,
        AuthorityValue.ASSIGN_CONSULTANT_TO_ENQUIRY,
        AuthorityValue.USER_ADMIN
      })
  void getRegisteredEnquiries_Should_ReturnForbiddenAndCallNoMethods_When_NoConsultantAuthority()
      throws Exception {
    this.mvc
        .perform(
            get(ConversationControllerIT.GET_REGISTERED_ENQUIRIES_PATH)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(conversationListResolver);
  }

  @Test
  @WithMockUser(authorities = {USER_DEFAULT})
  void getRegisteredEnquiries_Should_ReturnForbiddenAndCallNoMethods_When_NoCsrfToken()
      throws Exception {
    this.mvc
        .perform(
            get(ConversationControllerIT.GET_REGISTERED_ENQUIRIES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(conversationListResolver);
  }
}
