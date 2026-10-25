package com.footballhub.security;

import java.util.Locale;

import com.footballhub.user.User;
import com.footballhub.user.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads users by username or email (both stored in lowercase). */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        String key = login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
        User user = (key.contains("@") ? users.findByEmail(key) : users.findByUsername(key))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return AppUserDetails.from(user);
    }
}
