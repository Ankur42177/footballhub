package com.footballhub.europe;

import com.footballhub.europe.ChampionsLeagueService.Fixtures;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChampionsLeagueController {

    private final ChampionsLeagueService service;

    public ChampionsLeagueController(ChampionsLeagueService service) {
        this.service = service;
    }

    /** Upcoming fixtures and recent results for Premier League clubs in the Champions League. */
    @GetMapping("/api/v1/champions-league/matches")
    public Fixtures matches() {
        return service.fixtures();
    }
}
