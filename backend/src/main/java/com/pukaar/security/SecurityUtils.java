package com.pukaar.security;

import com.pukaar.common.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException("UNAUTHORIZED", "Authentication required");
        }
        return principal;
    }

    public static UUID currentUserId() {
        return currentUser().getId();
    }
}
