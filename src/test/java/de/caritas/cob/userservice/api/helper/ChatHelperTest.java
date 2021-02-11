package de.caritas.cob.userservice.api.helper;

import static de.caritas.cob.userservice.testHelper.TestConstants.AGENCY_ID;
import static de.caritas.cob.userservice.testHelper.TestConstants.AGENCY_ID_2;
import static de.caritas.cob.userservice.testHelper.TestConstants.AGENCY_ID_3;
import static de.caritas.cob.userservice.testHelper.TestConstants.CHAT_DTO;
import static de.caritas.cob.userservice.testHelper.TestConstants.CONSULTANT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.repository.chat.Chat;
import de.caritas.cob.userservice.api.repository.chatAgency.ChatAgency;
import de.caritas.cob.userservice.api.repository.consultant.Consultant;
import de.caritas.cob.userservice.api.repository.consultantagency.ConsultantAgency;
import de.caritas.cob.userservice.api.repository.session.ConsultingType;
import de.caritas.cob.userservice.api.repository.user.User;
import de.caritas.cob.userservice.api.repository.userAgency.UserAgency;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ChatHelperTest {

  private ChatHelper chatHelper;
  @Mock
  private Chat chat;
  @Mock
  private Consultant consultant;
  @Mock
  private User user;
  private Set<ChatAgency> chatAgencySet;

  @Before
  public void setup() {
    chatHelper = new ChatHelper();
    ChatAgency[] chatAgencyArray = new ChatAgency[]{new ChatAgency(chat, AGENCY_ID_2)};
    chatAgencySet = new HashSet<ChatAgency>(Arrays.asList(chatAgencyArray));
    when(chat.getChatAgencies()).thenReturn(chatAgencySet);
  }

  /**
   * Method: isChatAgenciesContainConsultantAgency
   */
  @Test
  public void isChatAgenciesContainConsultantAgency_Should_ReturnTrue_WhenChatAgenciesContainConsultantAgency() {

    ConsultantAgency[] consultantAgencyArray =
        new ConsultantAgency[]{
            new ConsultantAgency(AGENCY_ID, consultant, AGENCY_ID, LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now()),
            new ConsultantAgency(AGENCY_ID_2, consultant, AGENCY_ID_2, LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now())};
    Set<ConsultantAgency> consultantAgencySet =
        new HashSet<ConsultantAgency>(Arrays.asList(consultantAgencyArray));

    when(consultant.getConsultantAgencies()).thenReturn(consultantAgencySet);

    assertTrue(chatHelper.isChatAgenciesContainConsultantAgency(chat, consultant));

  }

  @Test
  public void isChatAgenciesContainConsultantAgency_Should_ReturnFalse_WhenChatAgenciesNotContainConsultantAgency() {

    ConsultantAgency[] consultantAgencyArray =
        new ConsultantAgency[]{
            new ConsultantAgency(AGENCY_ID, consultant, AGENCY_ID, LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now()),
            new ConsultantAgency(AGENCY_ID_3, consultant, AGENCY_ID_3, LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now())};
    Set<ConsultantAgency> consultantAgencySet =
        new HashSet<ConsultantAgency>(Arrays.asList(consultantAgencyArray));

    when(consultant.getConsultantAgencies()).thenReturn(consultantAgencySet);

    assertFalse(chatHelper.isChatAgenciesContainConsultantAgency(chat, consultant));

  }

  /**
   * Method: isChatAgenciesContainUserAgency
   */
  @Test
  public void isChatAgenciesContainUserAgency_Should_ReturnTrue_WhenChatAgenciesContainUserAgency() {

    UserAgency[] userAgencyArray = new UserAgency[]{new UserAgency(AGENCY_ID, user, AGENCY_ID),
        new UserAgency(AGENCY_ID_2, user, AGENCY_ID_2)};
    Set<UserAgency> userAgencySet = new HashSet<UserAgency>(Arrays.asList(userAgencyArray));

    when(user.getUserAgencies()).thenReturn(userAgencySet);

    assertTrue(chatHelper.isChatAgenciesContainUserAgency(chat, user));

  }

  @Test
  public void isChatAgenciesContainUserAgency_Should_ReturnFalse_WhenChatAgenciesNotContainUserAgency() {

    UserAgency[] userAgencyArray = new UserAgency[]{new UserAgency(AGENCY_ID, user, AGENCY_ID),
        new UserAgency(AGENCY_ID_3, user, AGENCY_ID_3)};
    Set<UserAgency> userAgencySet = new HashSet<UserAgency>(Arrays.asList(userAgencyArray));

    when(user.getUserAgencies()).thenReturn(userAgencySet);

    assertFalse(chatHelper.isChatAgenciesContainUserAgency(chat, user));

  }


  /**
   * Method: convertChatDTOtoChat
   */
  @Test
  public void convertChatDTOtoChat_Should_ReturnValidChat() {

    Chat result = chatHelper.convertChatDTOtoChat(CHAT_DTO, CONSULTANT);

    assertEquals(CHAT_DTO.getDuration(), result.getDuration());
    assertEquals(CHAT_DTO.getTopic(), result.getTopic());
    assertEquals(CONSULTANT, result.getChatOwner());
    assertEquals(ConsultingType.KREUZBUND, result.getConsultingType());
    assertEquals(CHAT_DTO.isRepetitive(), result.isRepetitive());
    LocalDateTime startDate = LocalDateTime.of(CHAT_DTO.getStartDate(), CHAT_DTO.getStartTime());
    assertEquals(startDate, result.getStartDate());
    assertEquals(startDate, result.getInitialStartDate());

  }
}
