package com.footballhub.europe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.footballhub.club.Club;
import com.footballhub.club.ClubRepository;
import com.footballhub.config.AppProperties;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class ChampionsLeagueServiceTest {

    private static final String SAMPLE = """
            {"matches": [
              {"id": 1, "utcDate": "2026-11-04T20:00:00Z", "status": "TIMED", "stage": "LEAGUE_STAGE", "matchday": 4,
               "homeTeam": {"name": "Manchester City FC"}, "awayTeam": {"name": "FC Barcelona"},
               "score": {"fullTime": {"home": null, "away": null}}},
              {"id": 2, "utcDate": "2026-11-05T20:00:00Z", "status": "SCHEDULED", "stage": "LEAGUE_STAGE", "matchday": 4,
               "homeTeam": {"name": "Real Madrid CF"}, "awayTeam": {"name": "Bayern München"},
               "score": {"fullTime": {"home": null, "away": null}}},
              {"id": 3, "utcDate": "2026-10-21T19:00:00Z", "status": "FINISHED", "stage": "LEAGUE_STAGE", "matchday": 3,
               "homeTeam": {"name": "Inter"}, "awayTeam": {"name": "Liverpool FC"},
               "score": {"fullTime": {"home": 1, "away": 2}}},
              {"id": 4, "utcDate": "2026-10-28T20:00:00Z", "status": "TIMED", "stage": "LEAGUE_STAGE", "matchday": 4,
               "homeTeam": {"name": "Brighton & Hove Albion FC"}, "awayTeam": {"name": null},
               "score": {"fullTime": {"home": null, "away": null}}}
            ]}
            """;

    private final ClubRepository clubs = mock(ClubRepository.class);

    ChampionsLeagueServiceTest() {
        List<Club> premierLeague = List.of(club("Manchester City", "manchester-city"), club("Liverpool", "liverpool"),
                club("Brighton & Hove Albion", "brighton-and-hove-albion"));
        when(clubs.findAll()).thenReturn(premierLeague);
    }

    @Test
    void keepsOnlyMatchesWithPremierLeagueClubs() {
        var service = new ChampionsLeagueService(new AppProperties.FootballData("key"), clubs, JsonMapper.builder().build(),
                url -> SAMPLE);
        var fixtures = service.fixtures();

        assertThat(fixtures.configured()).isTrue();
        assertThat(fixtures.upcoming()).extracting(ChampionsLeagueService.Match::id).containsExactly(4L, 1L);
        assertThat(fixtures.upcoming().get(1).home().clubSlug()).isEqualTo("manchester-city");
        assertThat(fixtures.upcoming().get(1).away().clubSlug()).isNull();
        assertThat(fixtures.upcoming().get(0).away().name()).isEqualTo("To be decided");
        assertThat(fixtures.upcoming().get(1).stage()).isEqualTo("League stage");

        assertThat(fixtures.recent()).hasSize(1);
        var result = fixtures.recent().get(0);
        assertThat(result.away().clubSlug()).isEqualTo("liverpool");
        assertThat(result.homeGoals()).isEqualTo(1);
        assertThat(result.awayGoals()).isEqualTo(2);
        assertThat(result.kickOff()).isEqualTo(Instant.parse("2026-10-21T19:00:00Z"));
    }

    @Test
    void cachesAndSurvivesProviderErrors() {
        AtomicInteger calls = new AtomicInteger();
        var service = new ChampionsLeagueService(new AppProperties.FootballData("key"), clubs, JsonMapper.builder().build(),
                url -> {
                    if (calls.incrementAndGet() > 1) {
                        throw new IllegalStateException("down");
                    }
                    return SAMPLE;
                });
        assertThat(service.fixtures().upcoming()).hasSize(2);
        assertThat(service.fixtures().upcoming()).hasSize(2);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void reportsNotConfiguredWithoutAKey() {
        var service = new ChampionsLeagueService(new AppProperties.FootballData(""), clubs, JsonMapper.builder().build(),
                url -> { throw new AssertionError("must not call the provider"); });
        assertThat(service.fixtures().configured()).isFalse();
    }

    @Test
    void normalisesClubNames() {
        assertThat(ChampionsLeagueService.normalise("Brighton & Hove Albion FC")).isEqualTo("brighton and hove albion");
        assertThat(ChampionsLeagueService.normalise("AFC Bournemouth")).isEqualTo("bournemouth");
        assertThat(ChampionsLeagueService.normalise("Bayern München")).isEqualTo("bayern munchen");
    }

    private static Club club(String name, String slug) {
        Club club = mock(Club.class);
        when(club.getName()).thenReturn(name);
        when(club.getSlug()).thenReturn(slug);
        return club;
    }
}
