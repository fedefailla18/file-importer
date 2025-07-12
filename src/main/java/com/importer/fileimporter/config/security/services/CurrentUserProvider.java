package com.importer.fileimporter.config.security.services;

import com.importer.fileimporter.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class to get the currently authenticated user.
 */
@Component
public class CurrentUserProvider {

    /**
     * Get the currently authenticated user.
     *
     * @return the currently authenticated user, or null if no user is authenticated
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        
        return null;
    }
}