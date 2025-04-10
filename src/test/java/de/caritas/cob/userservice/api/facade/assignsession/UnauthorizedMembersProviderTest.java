package de.caritas.cob.userservice.api.facade.assignsession;

import static de.caritas.cob.userservice.api.testHelper.FieldConstants.FIELD_NAME_ROCKET_CHAT_SYSTEM_USER_ID;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.ROCKET_CHAT_SYSTEM_USER_ID;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.SESSION_WITH_ASKER_AND_CONSULTANT;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import de.caritas.cob.userservice.api.adapters.rocketchat.RocketChatCredentials;
import de.caritas.cob.userservice.api.adapters.rocketchat.RocketChatCredentialsProvider;
import de.caritas.cob.userservice.api.adapters.rocketchat.dto.group.GroupMemberDTO;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatUserNotInitializedException;
import de.caritas.cob.userservice.api.model.Consultant;
import de.caritas.cob.userservice.api.service.ConsultantService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UnauthorizedMembersProviderTest {

  @InjectMocks UnauthorizedMembersProvider unauthorizedMembersProvider;

  @Mock ConsultantService consultantService;

  @Mock RocketChatCredentialsProvider rocketChatCredentialsProvider;

  EasyRandom easyRandom = new EasyRandom();
  Consultant newConsultant = easyRandom.nextObject(Consultant.class);
  Consultant normalConsultant = easyRandom.nextObject(Consultant.class);
  Consultant mainConsultant = easyRandom.nextObject(Consultant.class);
  Consultant mainConsultant2 = easyRandom.nextObject(Consultant.class);
  RocketChatCredentials techUserRcCredentials = easyRandom.nextObject(RocketChatCredentials.class);
  List<GroupMemberDTO> initialMemberList;

  @BeforeEach
  void setup() throws SecurityException {
    setField(
        unauthorizedMembersProvider,
        FIELD_NAME_ROCKET_CHAT_SYSTEM_USER_ID,
        ROCKET_CHAT_SYSTEM_USER_ID);
    newConsultant.setRocketChatId("newConsultantRcId");
    normalConsultant.setRocketChatId("normalConsultantRcId");
    mainConsultant.setRocketChatId("mainConsultantRcId");
    mainConsultant2.setRocketChatId("mainConsultantRcId2");
    techUserRcCredentials.setRocketChatUserId("techUserRcId");
    initialMemberList =
        asList(
            new GroupMemberDTO("userRcId", null, "name", null, null),
            new GroupMemberDTO("newConsultantRcId", null, "name", null, null),
            new GroupMemberDTO("normalConsultantRcId", null, "name", null, null),
            new GroupMemberDTO("otherRcId", null, "name", null, null),
            new GroupMemberDTO("otherRcId2", null, "name", null, null),
            new GroupMemberDTO("mainConsultantRcId", null, "name", null, null),
            new GroupMemberDTO("mainConsultantRcId2", null, "name", null, null),
            new GroupMemberDTO("rcTechnicalRcId", null, "name", null, null),
            new GroupMemberDTO(ROCKET_CHAT_SYSTEM_USER_ID, null, "name", null, null),
            new GroupMemberDTO("techUserRcId", null, "name", null, null));
    List.of(newConsultant, normalConsultant, mainConsultant, mainConsultant2)
        .forEach(
            consultant ->
                when(consultantService.getConsultantByRcUserId(consultant.getRocketChatId()))
                    .thenReturn(Optional.of(consultant)));
  }

  @Test
  void obtainConsultantsToRemoveShouldNotIncludeConsultantToAssignIfNotAssignedAlready()
      throws RocketChatUserNotInitializedException {

    var consultant = easyRandom.nextObject(Consultant.class);
    when(rocketChatCredentialsProvider.getTechnicalUser()).thenReturn(techUserRcCredentials);

    var consultantsToRemove =
        unauthorizedMembersProvider.obtainConsultantsToRemove(
            RC_GROUP_ID, SESSION_WITH_ASKER_AND_CONSULTANT, consultant, initialMemberList);

    consultantsToRemove.forEach(
        consultantToRemove -> {
          assertNotEquals(consultantToRemove.getId(), consultant.getId());
          assertNotEquals(consultantToRemove.getRocketChatId(), consultant.getRocketChatId());
        });
  }

  @Test
  void obtainConsultantsToRemoveShouldNotIncludeConsultantToAssignIfAlreadyAssigned()
      throws RocketChatUserNotInitializedException {

    var consultant = easyRandom.nextObject(Consultant.class);
    when(rocketChatCredentialsProvider.getTechnicalUser()).thenReturn(techUserRcCredentials);
    var memberList = new ArrayList<>(initialMemberList);
    var consultantAsGroupMember =
        new GroupMemberDTO(
            consultant.getRocketChatId(), null, consultant.getUsername(), null, null);
    memberList.add(consultantAsGroupMember);

    var consultantsToRemove =
        unauthorizedMembersProvider.obtainConsultantsToRemove(
            RC_GROUP_ID, SESSION_WITH_ASKER_AND_CONSULTANT, consultant, memberList);

    consultantsToRemove.forEach(
        consultantToRemove -> {
          assertNotEquals(consultantToRemove.getId(), consultant.getId());
          assertNotEquals(consultantToRemove.getRocketChatId(), consultant.getRocketChatId());
        });
  }

  @Test
  void obtainConsultantsToRemoveShouldNotIncludeConsultantToKeep()
      throws RocketChatUserNotInitializedException {

    var consultant = easyRandom.nextObject(Consultant.class);
    when(rocketChatCredentialsProvider.getTechnicalUser()).thenReturn(techUserRcCredentials);
    var memberList = new ArrayList<>(initialMemberList);
    var consultantAsGroupMember =
        new GroupMemberDTO(
            consultant.getRocketChatId(), null, consultant.getUsername(), null, null);
    memberList.add(consultantAsGroupMember);
    var consultantToKeep = easyRandom.nextObject(Consultant.class);
    var consultantToKeepAsGroupMember =
        new GroupMemberDTO(
            consultantToKeep.getRocketChatId(), null, consultantToKeep.getUsername(), null, null);
    memberList.add(consultantToKeepAsGroupMember);

    var consultantsToRemove =
        unauthorizedMembersProvider.obtainConsultantsToRemove(
            RC_GROUP_ID,
            SESSION_WITH_ASKER_AND_CONSULTANT,
            consultant,
            memberList,
            consultantToKeep);

    consultantsToRemove.forEach(
        consultantToRemove -> {
          assertNotEquals(consultantToRemove.getId(), consultantToKeep.getId());
          assertNotEquals(consultantToRemove.getRocketChatId(), consultantToKeep.getRocketChatId());
        });
  }
}
