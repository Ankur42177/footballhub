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
}
