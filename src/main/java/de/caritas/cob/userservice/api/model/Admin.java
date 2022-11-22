package de.caritas.cob.userservice.api.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.search.annotations.Field;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

/** Represents a consultant */
@Entity
@Table(name = "admin")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Admin implements Serializable {

  public enum AdminType {
    AGENCY,
    TENANT,
    SUPER
  }

  @Id
  @Column(name = "admin_id", updatable = false, nullable = false)
  @Size(max = 36)
  @NonNull
  private String adminId;

  @Column(name = "tenant_id")
  @Field
  private Long tenantId;

  @Column(name = "username", updatable = false, nullable = false)
  @Size(max = 255)
  @NonNull
  private String username;

  @Column(name = "first_name", updatable = false, nullable = false)
  @Size(max = 255)
  @NonNull
  private String firstName;

  @Column(name = "last_name", updatable = false, nullable = false)
  @Size(max = 255)
  @NonNull
  private String lastName;

  @Column(name = "email", nullable = false)
  @Size(max = 255)
  @NonNull
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(length = 6, nullable = false)
  private AdminType type;

  @Column(name = "rc_user_id")
  private String rcUserId;

  @Column(name = "id_old", updatable = false)
  private Long oldId;

  @CreatedDate
  @Column(name = "create_date", columnDefinition = "datetime")
  private LocalDateTime createDate;

  @LastModifiedDate
  @Column(name = "update_date", columnDefinition = "datetime")
  private LocalDateTime updateDate;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Admin)) {
      return false;
    }
    Admin admin = (Admin) o;
    return adminId.equals(admin.adminId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(adminId);
  }
}
