package com.footballhub.demo;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import com.footballhub.blog.Blog;
import com.footballhub.blog.BlogRepository;
import com.footballhub.club.ClubRepository;
import com.footballhub.common.util.Slugs;
import com.footballhub.config.AppProperties;
import com.footballhub.user.User;
import com.footballhub.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds an editorial account and a handful of articles when {@code SEED_DEMO_DATA=true}.
 * Idempotent: does nothing once the editorial account exists. The account gets a random
 * password nobody knows, so it cannot be used to sign in.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String EDITORIAL_USERNAME = "footballhub";

    private final AppProperties properties;
    private final UserRepository users;
    private final ClubRepository clubs;
    private final BlogRepository blogs;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(AppProperties properties, UserRepository users, ClubRepository clubs,
            BlogRepository blogs, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.clubs = clubs;
        this.blogs = blogs;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.seedDemoData() || users.existsByUsername(EDITORIAL_USERNAME)) {
            return;
        }
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        User editorial = new User(EDITORIAL_USERNAME, "editorial@footballhub.invalid",
                passwordEncoder.encode(Base64.getEncoder().encodeToString(secret)), "FootballHub Editorial");
        editorial.setBio("Tactics explainers and match analysis from the FootballHub desk.");
        editorial.setEmailVerifiedAt(Instant.now());
        users.save(editorial);

        Instant now = Instant.now();
        for (DemoArticles.Article article : DemoArticles.ALL) {
            Blog blog = new Blog(editorial, clubs.findBySlug(article.clubSlug()).orElse(null), article.title(),
                    Slugs.slugify(article.title()), article.excerpt(), article.content().strip());
            blog.publish();
            blog.setPublishedAt(now.minus(Duration.ofDays(article.daysAgo())));
            blogs.save(blog);
        }
        log.info("Seeded demo content: {} articles", DemoArticles.ALL.size());
    }
}
