package com.nubea.spring.modules.auth.platform.entities;

import com.nubea.spring.shared.utils.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "platform_user")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PlatformUser {
  @Id
  @GeneratedUuidV7
  @Column(name = "user_id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID userId;

  @Column(name = "first_names", length = 100, nullable = false)
  private String firstNames;

  @Column(name = "last_names", length = 100, nullable = false)
  private String lastNames;

  @Column(name = "email", length = 150, nullable = false)
  private String email;

  @Column(name = "password", columnDefinition = "text", nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private PlatformUserRoles role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private PlatformUserStatus status = PlatformUserStatus.ACTIVO;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
    if (this.status == null) {
      this.status = PlatformUserStatus.ACTIVO;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
