package com.nubea.spring.modules.auth.platform.services;

import com.nubea.spring.modules.auth.platform.dtos.RegisterPlatformUserDto;
import com.nubea.spring.modules.auth.platform.dtos.UserResponseDto;
import com.nubea.spring.modules.auth.platform.entities.PlatformUser;
import com.nubea.spring.modules.auth.platform.entities.PlatformUserRoles;
import com.nubea.spring.modules.auth.platform.errors.PlatformUserAlreadyExists;
import com.nubea.spring.modules.auth.platform.repositories.AuthPlatformRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterPlatformUser {
  private final AuthPlatformRepository repo;
  private final PasswordEncoder encoder;

  public UserResponseDto execute(RegisterPlatformUserDto dto) {
    if(repo.findByEmail(dto.email().trim().toLowerCase()).isPresent()) {
      throw new PlatformUserAlreadyExists("User already exists");
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
