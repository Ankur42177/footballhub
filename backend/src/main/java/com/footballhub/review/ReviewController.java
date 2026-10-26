package com.footballhub.review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.footballhub.common.dto.PageResponse;
import com.footballhub.review.ReviewService.PendingReview;
import com.footballhub.review.ReviewService.ReviewDecision;
import com.footballhub.review.ReviewService.ReviewPreview;
import com.footballhub.security.AppUserDetails;
import com.footballhub.security.CurrentUser;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Review endpoints. The emailed link's token is the credential for the public ones, and it
 * always travels in a POST body (never in a URL) so it can't appear in access logs. The
 * admin endpoints are restricted to the ADMIN role in SecurityConfig and checked again in
 * the service.
 */
@RestController
public class ReviewController {

    public record TokenRequest(@NotBlank @Size(max = 200) String token) {
    }

    public record LinkDecisionRequest(@NotBlank @Size(max = 200) String token, @Size(max = 500) String note) {
    }

    public record AdminDecisionRequest(@Size(max = 500) String note) {
    }

    private final ReviewService reviews;
    private final CurrentUser currentUser;

    public ReviewController(ReviewService reviews, CurrentUser currentUser) {
        this.reviews = reviews;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/v1/reviews/preview")
    public ReviewPreview preview(@Valid @RequestBody TokenRequest request) {
        return reviews.preview(request.token());
    }

    @PostMapping("/api/v1/reviews/approve")
    public ReviewDecision approveByLink(@Valid @RequestBody LinkDecisionRequest request) {
        return reviews.decideByLink(request.token(), true, request.note());
    }

    @PostMapping("/api/v1/reviews/reject")
    public ReviewDecision rejectByLink(@Valid @RequestBody LinkDecisionRequest request) {
        return reviews.decideByLink(request.token(), false, request.note());
    }

    @GetMapping("/api/v1/admin/reviews")
    public PageResponse<PendingReview> pending(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @AuthenticationPrincipal AppUserDetails principal) {
        currentUser.require(principal);
        return reviews.pending(page, size);
    }

    @PostMapping("/api/v1/admin/reviews/{id}/approve")
    public ReviewDecision approve(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal,
            @Valid @RequestBody(required = false) AdminDecisionRequest request) {
        return reviews.decideAsAdmin(id, true, request == null ? null : request.note(), currentUser.require(principal));
    }

    @PostMapping("/api/v1/admin/reviews/{id}/reject")
    public ReviewDecision reject(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal,
            @Valid @RequestBody(required = false) AdminDecisionRequest request) {
        return reviews.decideAsAdmin(id, false, request == null ? null : request.note(), currentUser.require(principal));
    }
}
