package de.caritas.cob.userservice.api.admin.service.rocketchat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.adapters.keycloak.KeycloakService;
import de.caritas.cob.userservice.api.manager.consultingtype.ConsultingTypeManager;
import de.caritas.cob.userservice.api.model.Consultant;
import de.caritas.cob.userservice.api.model.Session;
import de.caritas.cob.userservice.api.model.Session.SessionStatus;
import de.caritas.cob.userservice.api.port.out.IdentityClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RocketChatOperationConditionProviderTest {

  @InjectMocks private RocketChatOperationConditionProvider conditionProvider;

  @Mock private KeycloakService keycloakService;

  @Mock private Session session;
  @Mock private Consultant consultant;
  @Mock private IdentityClient identityClient;

  @Mock private ConsultingTypeManager consultingTypeManager;

  @Test
  void canAddToRocketChatGroup_Should_returnTrue_When_sessionIsAnEnquiry() {
    when(this.session.getStatus()).thenReturn(SessionStatus.NEW);

    boolean result = this.conditionProvider.canAddToRocketChatGroup();

    assertThat(result, is(true));
  }

  @Test
  void canAddToRocketChatGroup_Should_returnFalse_When_sessionIsInitial() {
    when(this.session.getStatus()).thenReturn(SessionStatus.INITIAL);

    boolean result = this.conditionProvider.canAddToRocketChatGroup();

    assertThat(result, is(false));
  }

  @Test
  void canAddToRocketChatGroup_Should_returnFalse_When_sessionIsInProgress() {
    when(this.session.getStatus()).thenReturn(SessionStatus.IN_PROGRESS);

    boolean result = this.conditionProvider.canAddToRocketChatGroup();

    assertThat(result, is(false));
  }
}
