package com.example.parkio.util;

import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /** Returns the DB id of the currently authenticated user. */
    public Long getCurrentUserId() {
        String email = getCurrentEmail();
        return userRepository.findByEmailOrPhone(email, email)
                .orElseThrow(() -> ParkioException.notFound("Authenticated user not found"))
                .getId();
    }

    /** Returns the email (subject) of the currently authenticated user. */
    public String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw ParkioException.forbidden("Not authenticated");
        }
        return auth.getName();
    }

    /** True if the current user carries ROLE_ADMIN (super-admin — superset of every internal permission). */
    public boolean isAdmin() {
        return hasAuthority("ROLE_ADMIN");
    }

    public boolean isOps() {
        return hasAuthority("ROLE_OPS");
    }

    public boolean isCompliance() {
        return hasAuthority("ROLE_COMPLIANCE");
    }

    public boolean isSupport() {
        return hasAuthority("ROLE_SUPPORT");
    }

    /** Any internal Parkio team role — as opposed to a driver or a parking company's own staff. */
    public boolean isInternalStaff() {
        return isAdmin() || isOps() || isCompliance() || isSupport();
    }

    private boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
