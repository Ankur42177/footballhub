package com.footballhub.security;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

import com.footballhub.user.Role;
import com.footballhub.user.User;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The authenticated principal stored in the server-side session. It is a snapshot:
 * services re-load the user (see {@link CurrentUser}) before any mutation so that a
 * suspension or role change takes effect immediately.
 */
public class AppUserDetails implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private String password;
    private final Role role;
    private final boolean enabled;

    public AppUserDetails(Long id, String username, String password, Role role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
    }

    public static AppUserDetails from(User user) {
        return new AppUserDetails(user.getId(), user.getUsername(), user.getPasswordHash(), user.getRole(),
                user.isEnabled());
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
