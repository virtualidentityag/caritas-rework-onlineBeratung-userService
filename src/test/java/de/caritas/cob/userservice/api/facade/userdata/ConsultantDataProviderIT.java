package de.caritas.cob.userservice.api.facade.userdata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.neovisionaries.i18n.LanguageCode;
import de.caritas.cob.userservice.api.UserServiceApplication;
import de.caritas.cob.userservice.api.helper.AuthenticatedUser;
import de.caritas.cob.userservice.api.manager.consultingtype.ConsultingTypeManager;
import de.caritas.cob.userservice.api.model.AgencyDTO;
import de.caritas.cob.userservice.api.repository.consultant.Consultant;
import de.caritas.cob.userservice.api.repository.consultant.Language;
import de.caritas.cob.userservice.api.service.agency.AgencyService;
import de.caritas.cob.userservice.consultingtypeservice.generated.web.model.ExtendedConsultingTypeResponseDTO;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = UserServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class ConsultantDataProviderIT {

  private final EasyRandom easyRandom = new EasyRandom();

  @Autowired
  private ConsultantDataProvider underTest;

  @MockBean
  private AgencyService agencyService;

  @MockBean
  @SuppressWarnings("unused")
  private AuthenticatedUser authenticatedUser;

  @MockBean
  private ConsultingTypeManager consultingTypeManager;

  @Test
  public void retrieveData_Should_returnDataWithHasArchiveTrue_When_ConsultantHasRegisteredSessions() {
    var consultant = easyRandom.nextObject(Consultant.class);
    consultant.setId("94c3e0b1-0677-4fd2-a7ea-56a71aefd0e8");
    when(agencyService.getAgencies(any()))
        .thenReturn(List.of(new AgencyDTO().consultingType(1)));
    when(consultingTypeManager.getConsultingTypeSettings(anyInt()))
        .thenReturn(new ExtendedConsultingTypeResponseDTO().isAnonymousConversationAllowed(false));

    var result = underTest.retrieveData(consultant);

    assertTrue(result.isHasArchive());
  }

  @Test
  public void retrieveData_Should_returnDataWithHasArchiveFalse_When_ConsultantHasNoRegisteredSessions() {
    var consultant = easyRandom.nextObject(Consultant.class);
    consultant.setId("34c3x5b1-0677-4fd2-a7ea-56a71aefd099");
    when(agencyService.getAgencies(any()))
        .thenReturn(List.of(new AgencyDTO().consultingType(1)));
    when(consultingTypeManager.getConsultingTypeSettings(anyInt()))
        .thenReturn(new ExtendedConsultingTypeResponseDTO().isAnonymousConversationAllowed(false));

    var result = underTest.retrieveData(consultant);

    assertFalse(result.isHasArchive());
  }

  @Test
  public void retrieveDataShouldMapMultipleConsultantLanguages() {
    var consultant = easyRandom.nextObject(Consultant.class);
    var languages = consultant.getLanguages().stream()
        .map(Language::getLanguageCode)
        .map(LanguageCode::name)
        .collect(Collectors.toSet());

    var result = underTest.retrieveData(consultant);

    assertEquals(languages, result.getLanguages());
  }

  @Test
  public void retrieveDataShouldMapDefaultConsultantLanguages() {
    var consultant = easyRandom.nextObject(Consultant.class);
    consultant.setLanguages(Set.of());

    var result = underTest.retrieveData(consultant);

    assertEquals(Set.of("de"), result.getLanguages());
  }
}
