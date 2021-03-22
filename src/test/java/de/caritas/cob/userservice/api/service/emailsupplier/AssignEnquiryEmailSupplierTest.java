package de.caritas.cob.userservice.api.service.emailsupplier;

import static de.caritas.cob.userservice.api.helper.EmailNotificationTemplates.TEMPLATE_ASSIGN_ENQUIRY_NOTIFICATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;

import de.caritas.cob.userservice.api.helper.UserHelper;
import de.caritas.cob.userservice.mailservice.generated.web.model.MailDTO;
import de.caritas.cob.userservice.mailservice.generated.web.model.TemplateDataDTO;
import de.caritas.cob.userservice.api.repository.consultant.Consultant;
import de.caritas.cob.userservice.api.service.ConsultantService;
import de.caritas.cob.userservice.api.service.LogService;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class AssignEnquiryEmailSupplierTest {

  private AssignEnquiryEmailSupplier assignEnquiryEmailSupplier;

  @Mock
  private Consultant receiverConsultant;

  @Mock
  private ConsultantService consultantService;

  @Mock
  private UserHelper userHelper;

  @Mock
  private Logger logger;

  @Before
  public void setup() {
    String applicationBaseUrl = "application base url";
    String askerUserName = "asker user name";
    String senderUserId = "sender user id";
    this.assignEnquiryEmailSupplier = new AssignEnquiryEmailSupplier(receiverConsultant,
        senderUserId, askerUserName, applicationBaseUrl, consultantService, userHelper);
    setInternalState(LogService.class, "LOGGER", logger);
  }

  @Test
  public void generateEmails_Should_ReturnEmptyListAndLogError_When_NoParametersAreProvided() {
    List<MailDTO> generatedMails = assignEnquiryEmailSupplier.generateEmails();

    assertThat(generatedMails, hasSize(0));
    verify(logger, times(1)).error(anyString(), anyString(), anyString());
  }

  @Test
  public void generateEmails_Should_ReturnEmptyListAndLogError_When_ReceiverIsValidAndSenderDoesntExist() {
    when(receiverConsultant.getEmail()).thenReturn("Valid email");
    when(consultantService.getConsultant(any())).thenReturn(Optional.empty());

    List<MailDTO> generatedMails = assignEnquiryEmailSupplier.generateEmails();

    assertThat(generatedMails, hasSize(0));
    verify(logger, times(1)).error(anyString(), anyString(), anyString());
  }

  @Test
  public void generateEmails_Should_ReturnExpectedMailDTO_When_ReceiverAndSenderIsValid() {
    when(receiverConsultant.getEmail()).thenReturn("Valid email");
    when(receiverConsultant.getFullName()).thenReturn("Moritz Mustermann");
    Consultant validConsultant = new Consultant();
    validConsultant.setFirstName("Max");
    validConsultant.setLastName("Mustermann");
    when(consultantService.getConsultant(any())).thenReturn(Optional.of(validConsultant));
    when(userHelper.decodeUsername(any())).thenReturn("The asker");

    List<MailDTO> generatedMails = assignEnquiryEmailSupplier.generateEmails();

    assertThat(generatedMails, hasSize(1));
    MailDTO generatedMail = generatedMails.get(0);
    assertThat(generatedMail.getTemplate(), is(TEMPLATE_ASSIGN_ENQUIRY_NOTIFICATION));
    assertThat(generatedMail.getEmail(), is("Valid email"));
    List<TemplateDataDTO> templateData = generatedMail.getTemplateData();
    assertThat(templateData, hasSize(4));
    assertThat(templateData.get(0).getKey(), is("name_sender"));
    assertThat(templateData.get(0).getValue(), is("Max Mustermann"));
    assertThat(templateData.get(1).getKey(), is("name_recipient"));
    assertThat(templateData.get(1).getValue(), is("Moritz Mustermann"));
    assertThat(templateData.get(2).getKey(), is("name_user"));
    assertThat(templateData.get(2).getValue(), is("The asker"));
    assertThat(templateData.get(3).getKey(), is("url"));
    assertThat(templateData.get(3).getValue(), is("application base url"));
  }

}
