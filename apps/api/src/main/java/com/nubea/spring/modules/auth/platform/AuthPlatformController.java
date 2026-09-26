package com.nubea.spring.modules.auth.platform;

import com.nubea.spring.modules.auth.platform.dtos.RegisterPlatformUserDto;
import com.nubea.spring.modules.auth.platform.dtos.UserResponseDto;
import com.nubea.spring.modules.auth.platform.services.RegisterPlatformUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth/platform")
@RequiredArgsConstructor
public class AuthPlatformController {
  private final RegisterPlatformUser register;

  @PostMapping("register")
  public ResponseEntity<UserResponseDto> register(
      @RequestBody @Valid RegisterPlatformUserDto req
  ) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(register.execute(req));
  }
}
