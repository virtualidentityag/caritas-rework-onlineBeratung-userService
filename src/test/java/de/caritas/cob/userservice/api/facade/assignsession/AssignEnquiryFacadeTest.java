package de.caritas.cob.userservice.api.facade.assignsession;

import static de.caritas.cob.userservice.api.model.Session.SessionStatus.IN_PROGRESS;
import static de.caritas.cob.userservice.api.testHelper.AsyncVerification.verifyAsync;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.CONSULTANT_WITH_AGENCY;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.LIST_GROUP_MEMBER_DTO;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.ROCKET_CHAT_SYSTEM_USER_ID;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.SESSION_WITHOUT_CONSULTANT;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.U25_SESSION_WITHOUT_CONSULTANT;
import static java.util.Arrays.asList;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;

import de.caritas.cob.userservice.api.adapters.keycloak.KeycloakService;
import de.caritas.cob.userservice.api.adapters.rocketchat.dto.group.GroupMemberDTO;
import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.facade.RocketChatFacade;
import de.caritas.cob.userservice.api.manager.consultingtype.ConsultingTypeManager;
import de.caritas.cob.userservice.api.model.Consultant;
import de.caritas.cob.userservice.api.model.ConsultantAgency;
import de.caritas.cob.userservice.api.model.Session;
import de.caritas.cob.userservice.api.model.Session.RegistrationType;
import de.caritas.cob.userservice.api.model.Session.SessionStatus;
import de.caritas.cob.userservice.api.service.LogService;
import de.caritas.cob.userservice.api.service.session.SessionService;
import de.caritas.cob.userservice.api.service.statistics.StatisticsService;
import de.caritas.cob.userservice.api.tenant.TenantContext;
import de.caritas.cob.userservice.api.tenant.TenantContextProvider;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class AssignEnquiryFacadeTest {
  public static final long CURRENT_TENANT_ID = 1L;

  @InjectMocks AssignEnquiryFacade assignEnquiryFacade;
  @Mock SessionService sessionService;
  @Mock RocketChatFacade rocketChatFacade;

  @Mock
  @SuppressWarnings("unused")
  KeycloakService keycloakService;

  @SuppressWarnings("unused")
  @Mock
  ConsultingTypeManager consultingTypeManager;

  @Mock SessionToConsultantVerifier sessionToConsultantVerifier;
  @Mock Logger logger;
  @Mock UnauthorizedMembersProvider unauthorizedMembersProvider;
  @Mock TenantContextProvider tenantContextProvider;
  @Mock StatisticsService statisticsService;
  @Mock HttpServletRequest httpServletRequest;

  @BeforeEach
  void setup() {
    setInternalState(LogService.class, "LOGGER", logger);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private void verifyConsultantAndSessionHaveBeenChecked(Session session, Consultant consultant) {
    verify(sessionToConsultantVerifier, times(1))
        .verifySessionIsNotInProgress(
            argThat(
                consultantSessionDTO ->
                    consultantSessionDTO.getConsultant().equals(consultant)
                        && consultantSessionDTO.getSession().equals(session)));
    verify(sessionToConsultantVerifier, times(1))
        .verifyPreconditionsForAssignment(
            argThat(
                consultantSessionDTO ->
                    consultantSessionDTO.getConsultant().equals(consultant)
                        && consultantSessionDTO.getSession().equals(session)),
            Mockito.eq(false));
  }

  @Test
  void assignEnquiry_Should_ReturnOKAndRemoveSystemMessagesFromGroup() {
    // given
    TenantContext.setCurrentTenant(CURRENT_TENANT_ID);
    when(rocketChatFacade.retrieveRocketChatMembers(anyString())).thenReturn(LIST_GROUP_MEMBER_DTO);

    // when
    assignEnquiryFacade.assignRegisteredEnquiry(SESSION_WITHOUT_CONSULTANT, CONSULTANT_WITH_AGENCY);

    // then
    verifyConsultantAndSessionHaveBeenChecked(SESSION_WITHOUT_CONSULTANT, CONSULTANT_WITH_AGENCY);
    verify(rocketChatFacade, times(0)).removeUserFromGroup(ROCKET_CHAT_SYSTEM_USER_ID, RC_GROUP_ID);
    verifyAsync(
        (a) ->
            verify(rocketChatFacade, times(1))
                .removeSystemMessagesFromRocketChatGroup(anyString()));
    verifyAsync(
        (a) -> verify(tenantContextProvider).setCurrentTenantContextIfMissing(CURRENT_TENANT_ID));
  }

  @Test
  void assignEnquiry_Should_LogError_When_RCRemoveGroupMembersFails() {
    doThrow(new InternalServerErrorException(""))
        .when(rocketChatFacade)
        .removeSystemMessagesFromRocketChatGroup(anyString());

    assignEnquiryFacade.assignRegisteredEnquiry(
        U25_SESSION_WITHOUT_CONSULTANT, CONSULTANT_WITH_AGENCY);

    verifyConsultantAndSessionHaveBeenChecked(
        U25_SESSION_WITHOUT_CONSULTANT, CONSULTANT_WITH_AGENCY);
    verify(sessionService, times(1))
        .updateConsultantAndStatusForSession(
            U25_SESSION_WITHOUT_CONSULTANT, CONSULTANT_WITH_AGENCY, SessionStatus.IN_PROGRESS);
    verifyAsync(
        (a) -> verify(logger, times(1)).error(anyString(), anyString(), anyString(), anyString()));
  }

  @Test
  void assignEnquiry_Should_LogError_WhenRemoveSystemMessagesFromGroupFails() {
    doThrow(new InternalServerErrorException("error"))
        .when(rocketChatFacade)
        .removeSystemMessagesFromRocketChatGroup(Mockito.any());

    assignEnquiryFacade.assignRegisteredEnquiry(
        U25_SESSION_WITHOUT_CONSULTANT, CONSULTANT_WITH_AGENCY);

    verifyConsultantAndSessionHaveBeenChecked(
        U25_SESSION_WITHOUT_CONSULTANT, CONSULTANT_WITH_AGENCY);
    verifyAsync(
        (a) -> verify(logger, times(1)).error(anyString(), anyString(), anyString(), anyString()));
    verify(sessionService, times(1))
        .updateConsultantAndStatusForSession(
            U25_SESSION_WITHOUT_CONSULTANT, CONSULTANT_WITH_AGENCY, IN_PROGRESS);
  }

  @Test
  void assignEnquiry_Should_removeAllUnauthorizedMembers() {
    Session session = new EasyRandom().nextObject(Session.class);
    session.setStatus(SessionStatus.NEW);
    session.setConsultant(null);
    session.getUser().setRcUserId("userRcId");
    session.setRegistrationType(RegistrationType.REGISTERED);
    session.setAgencyId(CURRENT_TENANT_ID);
    ConsultantAgency consultantAgency = new EasyRandom().nextObject(ConsultantAgency.class);
    consultantAgency.setAgencyId(CURRENT_TENANT_ID);
    Consultant consultant = new EasyRandom().nextObject(Consultant.class);
    consultant.setConsultantAgencies(asSet(consultantAgency));
    consultant.setRocketChatId("consultantRcId");
    when(this.rocketChatFacade.retrieveRocketChatMembers(anyString()))
        .thenReturn(
            asList(
                new GroupMemberDTO("userRcId", null, "name", null, null),
                new GroupMemberDTO("consultantRcId", null, "name", null, null),
                new GroupMemberDTO("otherRcId", null, "name", null, null)));
    Consultant consultantToRemove = new EasyRandom().nextObject(Consultant.class);
    consultantToRemove.setRocketChatId("otherRcId");
    when(unauthorizedMembersProvider.obtainConsultantsToRemove(any(), any(), any(), any()))
        .thenReturn(List.of(consultantToRemove));

    this.assignEnquiryFacade.assignRegisteredEnquiry(session, consultant);

    verifyConsultantAndSessionHaveBeenChecked(session, consultant);
    verifyAsync(
        (a) ->
            verify(this.rocketChatFacade, times(1))
                .removeUserFromGroupIgnoreGroupNotFound(
                    consultantToRemove.getRocketChatId(), session.getGroupId()));
  }
}
