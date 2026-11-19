package com.ecommerce.backend.shared.security;

import java.util.Optional;
import java.util.UUID;

public interface CurrentUserProvider {
    Optional<UUID> getCurrentUserId();
    Optional<String> getCurrentUserEmail();
    boolean hasRole(String roleName);
}
