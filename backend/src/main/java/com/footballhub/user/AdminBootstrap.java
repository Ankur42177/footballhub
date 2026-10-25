package com.footballhub.user;

import com.footballhub.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promotes existing accounts whose email is listed in {@code ADMIN_EMAILS}, so the site
 * owner can become an administrator without editing the database by hand.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final AppProperties properties;

    public AdminBootstrap(UserRepository users, AppProperties properties) {
        this.users = users;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String email : properties.adminEmails()) {
            users.findByEmail(email).ifPresent(user -> {
                if (user.getRole() != Role.ADMIN) {
                    user.setRole(Role.ADMIN);
                    log.info("Promoted {} to ADMIN from ADMIN_EMAILS", user.getUsername());
                }
            });
        }
    }
}
