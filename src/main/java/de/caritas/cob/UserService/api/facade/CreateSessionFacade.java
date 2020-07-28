package de.caritas.cob.UserService.api.facade;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import de.caritas.cob.UserService.api.exception.CreateMonitoringException;
import de.caritas.cob.UserService.api.exception.ServiceException;
import de.caritas.cob.UserService.api.helper.AgencyHelper;
import de.caritas.cob.UserService.api.helper.AuthenticatedUser;
import de.caritas.cob.UserService.api.manager.consultingType.ConsultingTypeManager;
import de.caritas.cob.UserService.api.manager.consultingType.ConsultingTypeSettings;
import de.caritas.cob.UserService.api.model.AgencyDTO;
import de.caritas.cob.UserService.api.model.NewRegistrationDto;
import de.caritas.cob.UserService.api.model.NewRegistrationResponseDto;
import de.caritas.cob.UserService.api.model.UserDTO;
import de.caritas.cob.UserService.api.repository.session.ConsultingType;
import de.caritas.cob.UserService.api.repository.session.Session;
import de.caritas.cob.UserService.api.repository.session.SessionStatus;
import de.caritas.cob.UserService.api.repository.user.User;
import de.caritas.cob.UserService.api.service.LogService;
import de.caritas.cob.UserService.api.service.MonitoringService;
import de.caritas.cob.UserService.api.service.SessionDataService;
import de.caritas.cob.UserService.api.service.SessionService;

@Service
public class CreateSessionFacade {
  private final SessionService sessionService;
  private final ConsultingTypeManager consultingTypeManager;
  private final AgencyHelper agencyHelper;
  private final MonitoringService monitoringService;
  private final SessionDataService sessionDataService;
  private final LogService logService;

  @Autowired
  public CreateSessionFacade(SessionService sessionService,
      ConsultingTypeManager consultingTypeManager, AgencyHelper agencyHelper,
      MonitoringService monitoringService, SessionDataService sessionDataService,
      LogService logService) {
    this.sessionService = sessionService;
    this.consultingTypeManager = consultingTypeManager;
    this.agencyHelper = agencyHelper;
    this.monitoringService = monitoringService;
    this.sessionDataService = sessionDataService;
    this.logService = logService;
  }

  /**
   * Creates a new {@link Session} for the currently {@link AuthenticatedUser} if this user does not
   * already have a session for the provided {@link ConsultingType}.
   * 
   * @param newRegistrationDto {@link NewRegistrationDto}
   * @return
   *         <ul>
   *         <li>{@link HttpStatus#CREATED}</li> if successfully registered
   *         <li>{@link HttpStatus#CONFLICT}</li> if already registered to provided
   *         {@link ConsultingType}
   *         <li>{@link HttpStatus#INTERNAL_SERVER_ERROR}</li> on unexpected errors
   *         </ul>
   */
  /**
   * Creates a new {@link Session} for the currently {@link AuthenticatedUser} if this user does not
   * already have a session for the provided {@link ConsultingType}.
   * 
   * @param newRegistrationDto {@link NewRegistrationDto}
   * @return {@link NewRegistrationResponseDto} with {@link HttpStatus} and ID of the created
   *         session
   */
  public NewRegistrationResponseDto createSession(NewRegistrationDto newRegistrationDto,
      User user) {

    ConsultingType consultingType =
        ConsultingType.values()[Integer.valueOf(newRegistrationDto.getConsultingType())];

    if (isRegisteredToConsultingType(user, consultingType)) {
      return NewRegistrationResponseDto.builder().status(HttpStatus.CONFLICT).build();
    }

    // Get agency and check if agency is assigned to given consulting type
    AgencyDTO agencyDto =
        agencyHelper.getVerifiedAgency(newRegistrationDto.getAgencyId(), consultingType);
    Session session = null;

    if (agencyDto == null) {
      return NewRegistrationResponseDto.builder().status(HttpStatus.BAD_REQUEST).build();
    }

    try {
      session = saveNewSession(newRegistrationDto, consultingType, agencyDto.isTeamAgency(), user);
      sessionDataService.saveSessionDataFromRegistration(session, new UserDTO());

    } catch (ServiceException serviceException) {
      logService.logCreateSessionFacadeError(
          String.format("Could not register new consulting type session with %s",
              newRegistrationDto.toString()),
          serviceException);

      if (session != null) {
        sessionService.deleteSession(session);
      }

      return NewRegistrationResponseDto.builder().status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    } catch (CreateMonitoringException createMonitoringException) {
      logService.logCreateSessionFacadeError(String.format(
          "Could not create monitoring while registering a new consulting type session with %s",
          newRegistrationDto.toString()), createMonitoringException);
      monitoringService.rollbackInitializeMonitoring(
          createMonitoringException.getExceptionInformation().getSession());

      return NewRegistrationResponseDto.builder().status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    return NewRegistrationResponseDto.builder().sessionId(session.getId())
        .status(HttpStatus.CREATED).build();
  }

  /**
   * Saves the new {@link Session} and creates the initial monitoring.
   * 
   * @param newRegistrationDto {@link NewRegistrationDto}
   * @param consultingType {@link ConsultingType}
   * @param isTeamAgency {@link AgencyDTO#isTeamAgency()}
   * @throws CreateMonitoringException when initialization of monitoring fails
   * @throws ServiceException when saving the {@link Session} fails
   */
  /**
   * Saves the new {@link Session} and creates the initial monitoring.
   * 
   * @param newRegistrationDto {@link NewRegistrationDto}
   * @param consultingType {@link ConsultingType}
   * @param isTeamAgency {@link AgencyDTO#isTeamAgency()}
   * @return the new registered {@link Session}
   * @throws CreateMonitoringException when initialization of monitoring fails
   * @throws ServiceException when saving the {@link Session} fails
   */
  private Session saveNewSession(NewRegistrationDto newRegistrationDto,
      ConsultingType consultingType, boolean isTeamAgency, User user)
      throws CreateMonitoringException, ServiceException {

    ConsultingTypeSettings consultingTypeSettings =
        consultingTypeManager.getConsultantTypeSettings(consultingType);
    Session session = null;

    session = sessionService.saveSession(new Session(user, consultingType,
        newRegistrationDto.getPostcode(), newRegistrationDto.getAgencyId(), SessionStatus.INITIAL,
        isTeamAgency, consultingTypeSettings.isMonitoring()));

    monitoringService.createMonitoring(session, consultingTypeSettings);

    return session;
  }

  /**
   * Checks if the given {@link User} is already registered within the given {@link ConsultingType}.
   * 
   * @param user {@link User}
   * @param consultingType {@link ConsultingType}
   * @return true if already registered, false if not
   */
  private boolean isRegisteredToConsultingType(User user, ConsultingType consultingType) {

    List<Session> sessions =
        sessionService.getSessionsForUserByConsultingType(user, consultingType);

    if (sessions.size() > 0) {
      return true;
    }

    return false;
  }
}
