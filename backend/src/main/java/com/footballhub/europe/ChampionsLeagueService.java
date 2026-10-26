package com.footballhub.europe;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.footballhub.club.Club;
import com.footballhub.club.ClubRepository;
import com.footballhub.config.AppProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Champions League fixtures and results involving Premier League clubs, from football-data.org.
 * Results are cached for 15 minutes (the free plan allows 10 requests a minute), and the
 * last good answer is kept if the provider is briefly unavailable. Only fixed, read-only
 * requests go to one known host, and the API key never leaves the server.
 */
@Service
public class ChampionsLeagueService {

    private static final Logger log = LoggerFactory.getLogger(ChampionsLeagueService.class);
    private static final Duration CACHE_FOR = Duration.ofMinutes(15);
    private static final String URL = "https://api.football-data.org/v4/competitions/CL/matches?dateFrom=%s&dateTo=%s";

    public record Team(String name, String clubSlug) {
    }

    public record Match(long id, Instant kickOff, String status, String stage, Integer matchday, Team home, Team away,
            Integer homeGoals, Integer awayGoals) {
    }

    public record Fixtures(boolean configured, Instant updatedAt, List<Match> upcoming, List<Match> recent) {
    }

    private final AppProperties.FootballData settings;
    private final ClubRepository clubs;
    private final JsonMapper json;
    private final Function<String, String> fetch;

    private volatile Fixtures cached;
    private volatile Instant cachedAt = Instant.EPOCH;

    @Autowired
    public ChampionsLeagueService(AppProperties properties, ClubRepository clubs, JsonMapper json) {
        this(properties.footballData(), clubs, json, null);
    }

    ChampionsLeagueService(AppProperties.FootballData settings, ClubRepository clubs, JsonMapper json,
            Function<String, String> fetch) {
        this.settings = settings;
        this.clubs = clubs;
        this.json = json;
        this.fetch = fetch != null ? fetch : this::download;
    }

    public Fixtures fixtures() {
        if (!settings.enabled()) {
            return new Fixtures(false, null, List.of(), List.of());
        }
        Fixtures current = cached;
        if (current != null && cachedAt.plus(CACHE_FOR).isAfter(Instant.now())) {
            return current;
        }
        synchronized (this) {
            if (cached != null && cachedAt.plus(CACHE_FOR).isAfter(Instant.now())) {
                return cached;
            }
            try {
                LocalDate today = LocalDate.now(ZoneOffset.UTC);
                String body = fetch.apply(URL.formatted(today.minusDays(21), today.plusDays(90)));
                cached = parse(body, Instant.now());
                cachedAt = Instant.now();
            }
            catch (RuntimeException ex) {
                log.warn("Champions League fixtures unavailable: {}", ex.getClass().getSimpleName());
                if (cached == null) {
                    return new Fixtures(true, null, List.of(), List.of());
                }
                cachedAt = Instant.now().minus(CACHE_FOR).plus(Duration.ofMinutes(2)); // retry in 2 minutes
            }
            return cached;
        }
    }

    Fixtures parse(String body, Instant now) {
        Map<String, String> premierLeague = clubs.findAll().stream()
                .collect(Collectors.toMap(club -> normalise(club.getName()), Club::getSlug, (a, b) -> a));
        List<Match> matches = new ArrayList<>();
        for (JsonNode node : json.readTree(body).path("matches")) {
            Team home = team(node.path("homeTeam"), premierLeague);
            Team away = team(node.path("awayTeam"), premierLeague);
            if (home.clubSlug() == null && away.clubSlug() == null) {
                continue;
            }
            JsonNode fullTime = node.path("score").path("fullTime");
            matches.add(new Match(node.path("id").asLong(), Instant.parse(node.path("utcDate").asString()),
                    node.path("status").asString(""), readable(node.path("stage").asString("")),
                    node.path("matchday").isNumber() ? node.path("matchday").asInt() : null, home, away,
                    fullTime.path("home").isNumber() ? fullTime.path("home").asInt() : null,
                    fullTime.path("away").isNumber() ? fullTime.path("away").asInt() : null));
        }
        List<Match> upcoming = matches.stream()
                .filter(match -> List.of("SCHEDULED", "TIMED", "IN_PLAY", "PAUSED").contains(match.status()))
                .sorted(Comparator.comparing(Match::kickOff)).limit(40).toList();
        List<Match> recent = matches.stream().filter(match -> "FINISHED".equals(match.status()))
                .sorted(Comparator.comparing(Match::kickOff).reversed()).limit(20).toList();
        return new Fixtures(true, now, upcoming, recent);
    }

    private static Team team(JsonNode node, Map<String, String> premierLeague) {
        String name = node.path("name").asString(node.path("shortName").asString("To be decided"));
        if (name.isBlank()) {
            name = "To be decided";
        }
        return new Team(name, premierLeague.get(normalise(name)));
    }

    /** "Brighton & Hove Albion FC" and "Brighton & Hove Albion" both become "brighton and hove albion". */
    static String normalise(String name) {
        String plain = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replace("&", " and ")
                .replaceAll("\\b(a?fc|cf)\\b", " ").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private static String readable(String stage) {
        if (stage.isEmpty()) {
            return "";
        }
        String words = stage.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private String download(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                    .header("X-Auth-Token", settings.apiKey()).GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("football-data.org answered " + response.statusCode());
            }
            return response.body();
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", ex);
        }
        catch (java.io.IOException ex) {
            throw new IllegalStateException("network error", ex);
        }
    }
}
