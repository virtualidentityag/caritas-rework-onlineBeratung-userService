package de.caritas.cob.userservice.api.facade.userdata;

import com.google.common.collect.Lists;
import de.caritas.cob.userservice.api.adapters.web.dto.UserDataResponseDTO;
import de.caritas.cob.userservice.api.helper.AuthenticatedUser;
import de.caritas.cob.userservice.api.port.out.IdentityClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

/** Provider for consultant information. */
@Component
@RequiredArgsConstructor
public class KeycloakUserDataProvider {

  private final @NonNull AuthenticatedUser authenticatedUser;
  private final @NonNull IdentityClient identityClient;

  public UserDataResponseDTO retrieveAuthenticatedUserData() {
    var user = identityClient.getById(authenticatedUser.getUserId());
    return userDataResponseDtoOf(user);
  }

  private UserDataResponseDTO userDataResponseDtoOf(UserRepresentation keycloakUser) {

    return UserDataResponseDTO.builder()
        .userId(keycloakUser.getId())
        .userName(keycloakUser.getUsername())
        .firstName(keycloakUser.getFirstName())
        .lastName(keycloakUser.getLastName())
        .email(keycloakUser.getEmail())
        .encourage2fa(false)
        .absenceMessage("")
        .isInTeamAgency(false)
        .agencies(Lists.newArrayList())
        .userRoles(authenticatedUser.getRoles())
        .grantedAuthorities(authenticatedUser.getGrantedAuthorities())
        .hasArchive(false)
        .build();
  }
}
