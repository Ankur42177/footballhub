package com.footballhub.security;

import com.footballhub.common.exception.ApiException;
import com.footballhub.user.Role;
import com.footballhub.user.User;
import com.footballhub.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Resolves the session principal to a fresh {@link User} row and enforces account
 * state and roles from the database rather than from the cached session.
 */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User require(AppUserDetails principal) {
        if (principal == null) {
            throw ApiException.unauthorized();
        }
        User user = users.findById(principal.getId()).orElseThrow(ApiException::unauthorized);
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account has been suspended.");
        }
        return user;
    }

    public User requireStaff(AppUserDetails principal) {
        User user = require(principal);
        if (!user.getRole().isStaff()) {
            throw ApiException.forbidden("Moderator access is required.");
        }
        return user;
    }

    public User requireAdmin(AppUserDetails principal) {
        User user = require(principal);
        if (user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Administrator access is required.");
        }
        return user;
    }
}
