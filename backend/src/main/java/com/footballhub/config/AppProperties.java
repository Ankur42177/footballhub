package com.footballhub.config;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application settings under the {@code app.*} prefix. See application.yml for the
 * environment variables that drive each value.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        List<String> corsAllowedOrigins,
        List<String> adminEmails,
        boolean requireVerifiedEmailToPublish,
        boolean seedDemoData,
        String featuredClub,
        Review review,
        Mail mail,
        RateLimit rateLimit) {

    public AppProperties {
        frontendUrl = (frontendUrl == null || frontendUrl.isBlank())
                ? "http://localhost:3000"
                : stripTrailingSlash(frontendUrl.trim());
        corsAllowedOrigins = corsAllowedOrigins == null ? List.of()
                : corsAllowedOrigins.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        adminEmails = adminEmails == null ? List.of()
                : adminEmails.stream().map(s -> s.trim().toLowerCase(Locale.ROOT)).filter(s -> !s.isEmpty()).toList();
        rateLimit = rateLimit == null ? new RateLimit(true, 20, 60) : rateLimit;
        featuredClub = featuredClub == null ? "" : featuredClub.trim().toLowerCase(Locale.ROOT);
        review = review == null ? new Review(null, 3, 7) : review;
        mail = mail == null ? new Mail(null, null, false) : mail;
    }

    /**
     * Origins the browser may call the API from. The site itself (frontendUrl) is always
     * included: requests reach the API through the frontend's /api/v1 rewrite, so the browser's
     * Origin header is the site's address even though the API runs on another host.
     */
    public List<String> allowedOrigins() {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(frontendUrl), corsAllowedOrigins.stream())
                .map(AppProperties::stripTrailingSlash)
                .distinct()
                .toList();
    }

    public boolean isAdminEmail(String email) {
        return email != null && adminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public record RateLimit(boolean enabled, int authPerMinute, int writesPerMinute) {
        public RateLimit {
            authPerMinute = authPerMinute <= 0 ? 20 : authPerMinute;
            writesPerMinute = writesPerMinute <= 0 ? 60 : writesPerMinute;
        }
    }

    /**
     * Editorial review of stories by non-admin authors.
     *
     * @param email         where review requests are sent (kept in configuration, not in code)
     * @param maxPending    how many stories one author may have waiting at once
     * @param linkValidDays how long a review link stays valid
     */
    public record Review(String email, int maxPending, int linkValidDays) {
        public Review {
            email = email == null ? "" : email.trim();
            maxPending = maxPending <= 0 ? 3 : maxPending;
            linkValidDays = linkValidDays <= 0 ? 7 : linkValidDays;
        }
    }

    /**
     * Outbound email. With a Resend API key, mail is sent through Resend's HTTPS API;
     * without one it is only logged. Message bodies (which may hold one-time links) are
     * logged only when logBodies is true, which is meant for local development.
     */
    public record Mail(String resendApiKey, String from, boolean logBodies) {
        public Mail {
            resendApiKey = resendApiKey == null ? "" : resendApiKey.trim();
            from = from == null || from.isBlank() ? "FootballHub <onboarding@resend.dev>" : from.trim();
        }
    }
}
