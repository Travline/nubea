package com.nubea.spring.modules.auth.platform.dtos;

import com.nubea.spring.modules.auth.platform.entities.PlatformUserRoles;
import com.nubea.spring.modules.auth.platform.entities.PlatformUserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDto(
    UUID userId,
    String firstNames,
    String lastNames,
    String email,
    PlatformUserRoles role
) { }
