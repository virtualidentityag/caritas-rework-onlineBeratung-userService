package de.caritas.cob.userservice.api.admin.service.agency;

import static de.caritas.cob.userservice.api.exception.httpresponses.customheader.HttpStatusExceptionReason.CONSULTANT_AGENCY_RELATION_DOES_NOT_EXIST;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.exception.httpresponses.CustomValidationHttpStatusException;
import de.caritas.cob.userservice.api.model.ConsultantAgency;
import de.caritas.cob.userservice.api.port.out.ConsultantAgencyRepository;
import de.caritas.cob.userservice.api.port.out.ConsultantRepository;
import de.caritas.cob.userservice.api.port.out.SessionRepository;
import de.caritas.cob.userservice.api.service.agency.AgencyService;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultantAgencyAdminUserServiceTest {

  @InjectMocks private ConsultantAgencyAdminService consultantAgencyAdminService;

  @Mock private ConsultantAgencyRepository consultantAgencyRepository;

  @Mock private ConsultantRepository consultantRepository;

  @Mock private SessionRepository sessionRepository;

  @Mock private AgencyService agencyService;

  @Mock private AgencyAdminService agencyAdminService;

  @Mock private ConsultantAgencyDeletionValidationService agencyDeletionValidationService;

  @Test
  void
      markConsultantAgencyForDeletion_Should_throwCustomValidationHttpStatusException_When_relationDoesNotExist() {
    try {
      this.consultantAgencyAdminService.markConsultantAgencyForDeletion("", 1L);
      fail("Exception was not thrown");
    } catch (CustomValidationHttpStatusException e) {
      assertThat(
          requireNonNull(e.getCustomHttpHeaders().get("X-Reason")).iterator().next(),
          is(CONSULTANT_AGENCY_RELATION_DOES_NOT_EXIST.name()));
    }
  }

  @Test
  void
      markConsultantAgencyForDeletion_Should_deleteConsultantAgency_When_consultantAgencyCanBeDeleted() {
    ConsultantAgency consultantAgency = new EasyRandom().nextObject(ConsultantAgency.class);
    consultantAgency.setDeleteDate(null);
    when(this.consultantAgencyRepository.findByConsultantIdAndAgencyIdAndDeleteDateIsNull(
            any(), any()))
        .thenReturn(singletonList(consultantAgency));

    this.consultantAgencyAdminService.markConsultantAgencyForDeletion("", 1L);

    assertThat(consultantAgency.getDeleteDate(), notNullValue());
    verify(this.consultantAgencyRepository).save(any(ConsultantAgency.class));
    verify(this.agencyDeletionValidationService).validateAndMarkForDeletion(any());
  }
}
