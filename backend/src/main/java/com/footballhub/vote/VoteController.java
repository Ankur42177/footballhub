package com.footballhub.vote;

import com.footballhub.security.AppUserDetails;
import com.footballhub.vote.VoteService.VoteResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/api/v1/blogs/{id}/vote")
    public VoteResponse vote(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return voteService.vote(id, principal);
    }

    @DeleteMapping("/api/v1/blogs/{id}/vote")
    public VoteResponse unvote(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return voteService.unvote(id, principal);
    }
}
