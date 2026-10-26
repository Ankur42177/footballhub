package com.footballhub.vote;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.footballhub.security.AppUserDetails;
import com.footballhub.vote.VoteService.VoteResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Upvotes are open to everyone: signed-in users vote as themselves, other visitors vote
 * through a random visitor cookie. Either way it's one vote per person per article.
 */
@RestController
public class VoteController {

    private final VoteService voteService;
    private final VisitorCookies visitors;

    public VoteController(VoteService voteService, VisitorCookies visitors) {
        this.voteService = voteService;
        this.visitors = visitors;
    }

    @GetMapping("/api/v1/blogs/{id}/vote")
    public VoteResponse status(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal,
            HttpServletRequest request) {
        return voteService.status(id, principal, visitors.read(request));
    }

    @PostMapping("/api/v1/blogs/{id}/vote")
    public VoteResponse vote(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal,
            HttpServletRequest request, HttpServletResponse response) {
        if (principal != null) {
            return voteService.vote(id, principal);
        }
        return voteService.voteAsVisitor(id, visitors.readOrCreate(request, response));
    }

    @DeleteMapping("/api/v1/blogs/{id}/vote")
    public VoteResponse unvote(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal,
            HttpServletRequest request) {
        if (principal != null) {
            return voteService.unvote(id, principal);
        }
        return voteService.unvoteAsVisitor(id, visitors.read(request));
    }
}
