package com.nubea.spring.modules.auth.platform.repositories;

import com.nubea.spring.modules.auth.platform.entities.PlatformUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AuthPlatformRepository extends JpaRepository<PlatformUser, UUID> {
  @Query("SELECT u FROM PlatformUser u WHERE u.email = :email AND u.status = 'ACTIVO'")
  Optional<PlatformUser> findByEmail(@Param("email") String email);
}
