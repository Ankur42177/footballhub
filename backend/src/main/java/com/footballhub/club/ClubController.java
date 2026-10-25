package com.footballhub.club;

import java.util.List;

import com.footballhub.club.dto.ClubDtos.ClubResponse;
import com.footballhub.club.dto.ClubDtos.FollowResponse;
import com.footballhub.security.AppUserDetails;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClubController {

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping("/api/v1/clubs")
    public List<ClubResponse> list(@AuthenticationPrincipal AppUserDetails viewer) {
        return clubService.listClubs(viewer);
    }

    @GetMapping("/api/v1/clubs/{slug}")
    public ClubResponse get(@PathVariable String slug, @AuthenticationPrincipal AppUserDetails viewer) {
        return clubService.getClub(slug, viewer);
    }

    @PostMapping("/api/v1/clubs/{id}/follow")
    public FollowResponse follow(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return clubService.follow(id, principal);
    }

    @DeleteMapping("/api/v1/clubs/{id}/follow")
    public FollowResponse unfollow(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return clubService.unfollow(id, principal);
    }

    @GetMapping("/api/v1/users/me/clubs")
    public List<ClubResponse> followed(@AuthenticationPrincipal AppUserDetails principal) {
        return clubService.followedClubs(principal);
    }
}
