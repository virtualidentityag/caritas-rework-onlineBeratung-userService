package de.caritas.cob.userservice.api.admin.service.rocketchat;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.adapters.keycloak.KeycloakService;
import de.caritas.cob.userservice.api.adapters.rocketchat.dto.group.GroupMemberDTO;
import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.facade.RocketChatFacade;
import de.caritas.cob.userservice.api.manager.consultingtype.ConsultingTypeManager;
import de.caritas.cob.userservice.api.model.Consultant;
import de.caritas.cob.userservice.api.model.Session;
import de.caritas.cob.userservice.api.model.Session.SessionStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RocketChatRemoveFromGroupOperationServiceTest {

  private RocketChatRemoveFromGroupOperationService removeService;

  @Mock private RocketChatFacade rocketChatFacade;

  @Mock private KeycloakService keycloakService;

  @Mock private Session session;

  @Mock private Consultant consultant;

  @Mock private ConsultingTypeManager consultingTypeManager;

  @BeforeEach
  void setup() {
    Map<Session, List<Consultant>> sessionConsultants = new HashMap<>();
    sessionConsultants.put(session, singletonList(consultant));
    this.removeService =
        RocketChatRemoveFromGroupOperationService.getInstance(
                this.rocketChatFacade, this.keycloakService, consultingTypeManager)
            .onSessionConsultants(sessionConsultants);
  }

  @Test
  void removeFromGroupsOrRollbackOnFailure_Should_notCollectMembers_When_rcUserIdIsEmpty() {
    this.removeService.removeFromGroupsOrRollbackOnFailure();

    verify(this.rocketChatFacade, times(0)).getStandardMembersOfGroup(any());
  }

  @Test
  void removeFromGroupsOrRollbackOnFailure_Should_executeRemove_When_rcUserIdIsGiven() {
    when(this.session.getGroupId()).thenReturn("group");
    when(this.consultant.getRocketChatId()).thenReturn("rcId");
    GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
    groupMemberDTO.set_id(this.consultant.getRocketChatId());
    when(this.rocketChatFacade.retrieveRocketChatMembers(any()))
        .thenReturn(singletonList(groupMemberDTO));

    this.removeService.removeFromGroupsOrRollbackOnFailure();

    verify(this.rocketChatFacade, times(1)).removeUserFromGroup("rcId", "group");
  }

  @Test
  void removeFromGroupsOrRollbackOnFailure_Should_throwInternalServerError_When_rollbackFails() {
    when(this.session.getGroupId()).thenReturn("group");
    when(this.session.getStatus()).thenReturn(SessionStatus.NEW);
    when(this.consultant.getRocketChatId()).thenReturn("rcId");
    GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
    groupMemberDTO.set_id(this.consultant.getRocketChatId());
    when(this.rocketChatFacade.retrieveRocketChatMembers(anyString()))
        .thenReturn(singletonList(groupMemberDTO));
    doThrow(new RuntimeException(""))
        .when(this.rocketChatFacade)
        .removeUserFromGroup(anyString(), anyString());
    doThrow(new RuntimeException(""))
        .when(this.rocketChatFacade)
        .addUserToRocketChatGroup(anyString(), anyString());

    try {
      this.removeService.removeFromGroupsOrRollbackOnFailure();
      fail("No Exception thrown");
    } catch (InternalServerErrorException e) {
      assertThat(e.getMessage(), containsString("ERROR: Failed to rollback"));
    }
  }

  @Test
  void
      removeFromGroupsOrRollbackOnFailure_Should_throwInternalServerErrorAndPerformRollback_When_error() {
    when(this.session.getGroupId()).thenReturn("group");
    when(this.session.getStatus()).thenReturn(SessionStatus.NEW);
    when(this.consultant.getRocketChatId()).thenReturn("rcId");
    GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
    groupMemberDTO.set_id(this.consultant.getRocketChatId());
    when(this.rocketChatFacade.retrieveRocketChatMembers(any()))
        .thenReturn(singletonList(groupMemberDTO));
    doThrow(new RuntimeException("")).when(this.rocketChatFacade).removeUserFromGroup(any(), any());

    try {
      this.removeService.removeFromGroupsOrRollbackOnFailure();
      fail("No Exception thrown");
    } catch (InternalServerErrorException e) {
      verify(this.rocketChatFacade, times(1)).addUserToRocketChatGroup("rcId", "group");
    }
  }

  @Test
  void removeFromGroupOrRollbackOnFailure_Should_executeRemoveForRocketChatGroup() {
    when(this.session.getGroupId()).thenReturn("group");
    when(this.consultant.getRocketChatId()).thenReturn("rcId");
    GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
    groupMemberDTO.set_id(this.consultant.getRocketChatId());
    when(this.rocketChatFacade.retrieveRocketChatMembers(any()))
        .thenReturn(singletonList(groupMemberDTO));

    this.removeService.removeFromGroupOrRollbackOnFailure();

    verify(this.rocketChatFacade, times(1)).removeUserFromGroupIgnoreGroupNotFound("rcId", "group");
  }

  @Test
  void removeFromGroupOrRollbackOnFailure_Should_throwInternalServerError_When_rollbackFails() {
    when(this.session.getGroupId()).thenReturn("group");
    when(this.session.getStatus()).thenReturn(SessionStatus.NEW);
    when(this.consultant.getRocketChatId()).thenReturn("rcId");
    GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
    groupMemberDTO.set_id(this.consultant.getRocketChatId());
    when(this.rocketChatFacade.retrieveRocketChatMembers(anyString()))
        .thenReturn(singletonList(groupMemberDTO));
    doThrow(new RuntimeException(""))
        .when(this.rocketChatFacade)
        .removeUserFromGroupIgnoreGroupNotFound(anyString(), anyString());
    doThrow(new RuntimeException(""))
        .when(this.rocketChatFacade)
        .addUserToRocketChatGroup(anyString(), anyString());

    try {
      this.removeService.removeFromGroupOrRollbackOnFailure();
      fail("No Exception thrown");
    } catch (InternalServerErrorException e) {
      assertThat(e.getMessage(), containsString("ERROR: Failed to rollback"));
    }
  }

  @Test
  void
      removeFromGroupOrRollbackOnFailure_Should_throwInternalServerErrorAndPerformRollback_When_error() {
    when(this.session.getGroupId()).thenReturn("group");
    when(this.session.getStatus()).thenReturn(SessionStatus.NEW);
    when(this.consultant.getRocketChatId()).thenReturn("rcId");
    GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
    groupMemberDTO.set_id(this.consultant.getRocketChatId());
    when(this.rocketChatFacade.retrieveRocketChatMembers(any()))
        .thenReturn(singletonList(groupMemberDTO));
    doThrow(new RuntimeException(""))
        .when(this.rocketChatFacade)
        .removeUserFromGroupIgnoreGroupNotFound(any(), any());

    try {
      this.removeService.removeFromGroupOrRollbackOnFailure();
      fail("No Exception thrown");
    } catch (InternalServerErrorException e) {
      verify(this.rocketChatFacade, times(1)).addUserToRocketChatGroup("rcId", "group");
    }
  }
}
