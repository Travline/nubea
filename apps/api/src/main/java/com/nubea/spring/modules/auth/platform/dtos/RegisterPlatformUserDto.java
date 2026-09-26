package com.nubea.spring.modules.auth.platform.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterPlatformUserDto(
    @NotBlank(message = "First name required")
    @Size(max = 100)
    String firstNames,

    @NotBlank(message = "Last name required")
    @Size(max = 100)
    String lastNames,

    @NotBlank(message = "Email required")
    @Size(max = 150)
    @Email(message = "User email format is invalid")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password too weak")
    String password
) {
}
