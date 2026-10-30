package com.nubea.spring.modules.auth.platform.services;

import com.nubea.spring.modules.auth.platform.dtos.RegisterPlatformUserDto;
import com.nubea.spring.modules.auth.platform.dtos.UserResponseDto;
import com.nubea.spring.modules.auth.platform.entities.PlatformUser;
import com.nubea.spring.modules.auth.platform.entities.PlatformUserRoles;
import com.nubea.spring.modules.auth.platform.repositories.AuthPlatformRepository;
import com.nubea.spring.shared.errors.AppError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterPlatformUser {
  private final AuthPlatformRepository repo;
  private final PasswordEncoder encoder;

  public UserResponseDto execute(RegisterPlatformUserDto dto) {
    if(repo.findByEmail(dto.email().trim().toLowerCase()).isPresent()) {
      throw new AppError(
              "USUARIO_EXISTENTE",
              HttpStatus.CONFLICT,
              String.format("El correo %s está en uso", dto.email().trim()),
              true
      );
    }

    var newUser = PlatformUser.builder()
        .firstNames(dto.firstNames().trim())
        .lastNames(dto.lastNames().trim())
        .email(dto.email().trim().toLowerCase())
        .password(encoder.encode(dto.password().trim()))
        .role(PlatformUserRoles.VENDEDOR)
        .build();

    var savedUser = repo.save(newUser);

    return new UserResponseDto(
        savedUser.getUserId(),
        savedUser.getFirstNames(),
        savedUser.getLastNames(),
        savedUser.getEmail(),
        savedUser.getRole()
    );
  }
}
