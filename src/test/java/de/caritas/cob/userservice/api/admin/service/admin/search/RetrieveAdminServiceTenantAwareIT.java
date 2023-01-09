package de.caritas.cob.userservice.api.admin.service.admin.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.caritas.cob.userservice.api.UserServiceApplication;
import de.caritas.cob.userservice.api.exception.httpresponses.NoContentException;
import de.caritas.cob.userservice.api.model.Admin;
import de.caritas.cob.userservice.api.model.Admin.AdminBase;
import de.caritas.cob.userservice.api.tenant.TenantContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = UserServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = "multitenancy.enabled=true")
@Transactional
public class RetrieveAdminServiceTenantAwareIT {

  private final String VALID_ADMIN_ID = "164be67d-4d1b-4d80-bb6b-0ee057a1c59e";

  @Autowired private RetrieveAdminService retrieveAdminService;

  @BeforeEach
  public void beforeTest() {
    TenantContext.setCurrentTenant(1L);
  }

  @AfterEach
  public void afterTests() {
    TenantContext.clear();
  }

  @Test
  public void findAgencyAdmin_Should_returnCorrectAdmin_When_correctIdIsProvided() {
    // given
    // when
    Admin admin = retrieveAdminService.findAdmin(VALID_ADMIN_ID, Admin.AdminType.AGENCY);

    // then
    assertThat(admin, notNullValue());
    assertThat(admin.getId(), is(VALID_ADMIN_ID));
  }

  @Test
  public void findAgencyAdmin_Should_throwNoContentException_When_incorrectIdIsProvided() {
    // given
    // when
    assertThrows(
        NoContentException.class,
        () -> retrieveAdminService.findAdmin("invalid", Admin.AdminType.AGENCY));
  }

  @Test
  public void findAgencyIdsOfAdmin_Should_returnCorrectAdmin_When_correctIdIsProvided() {
    // given
    long expectedAgencyId = 90L;

    // when
    List<Long> agencyIdsOfAdmin = retrieveAdminService.findAgencyIdsOfAdmin(VALID_ADMIN_ID);

    // then
    assertThat(agencyIdsOfAdmin, notNullValue());
    assertThat(agencyIdsOfAdmin, hasSize(1));
    assertThat(agencyIdsOfAdmin, hasItems(expectedAgencyId));
  }

  @Test
  public void findAllByInfix_Should_returnCorrectAdmin_When_correctIdInfix() {
    // given
    PageRequest pageable = PageRequest.of(0, 10);

    // when
    Page<AdminBase> admins = retrieveAdminService.findAllByInfix("Jeffy", pageable);

    // then
    assertThat(admins, notNullValue());
    assertThat(admins.getTotalElements(), is(1L));
  }

  @Test
  public void findAllByInfix_Should_returnCorrectAdminsOfTenant_When_correctIdInfix() {
    // given
    PageRequest pageable = PageRequest.of(0, 10);
    TenantContext.setCurrentTenant(2L);
    // when
    Page<AdminBase> admins = retrieveAdminService.findAllByInfix("Jeffy", pageable);

    // then
    assertThat(admins, notNullValue());
    assertThat(admins.getTotalElements(), is(3L));
  }

  @Test
  public void findAllById_Should_returnCorrectAdmin_When_correctIdsAreProvided() {
    // given
    Set<String> adminIds = new HashSet<>();
    adminIds.add("d42c2e5e-143c-4db1-a90f-7cccf82fbb15");
    adminIds.add("7ad454de-cf29-4557-b8b3-1bf986524de2");
    adminIds.add("6d15b3ff-2394-4d9f-9ea5-e958afe6a65c");

    // when
    List<Admin> admins = retrieveAdminService.findAllById(adminIds);

    // then
    assertThat(admins, notNullValue());
    assertThat(admins, hasSize(1));
  }
}
