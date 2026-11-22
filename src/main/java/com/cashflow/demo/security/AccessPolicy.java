package com.cashflow.demo.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("accessPolicy")
public class AccessPolicy {
    public boolean canAccess(Long userId, Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() &&
                (authentication.getName().equals(String.valueOf(userId)) ||
                        authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
}
