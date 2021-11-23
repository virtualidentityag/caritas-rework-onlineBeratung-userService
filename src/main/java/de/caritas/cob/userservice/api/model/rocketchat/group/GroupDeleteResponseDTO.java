package de.caritas.cob.userservice.api.model.rocketchat.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response object for Rocket.Chat API Call for deleting a group https://rocket.chat/docs/developer-guides/rest-api/groups/delete/
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDeleteResponseDTO {

  private boolean success;

}
