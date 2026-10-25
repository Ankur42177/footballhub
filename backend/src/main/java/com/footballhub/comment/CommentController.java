package com.footballhub.comment;

import com.footballhub.comment.dto.CommentDtos.CommentResponse;
import com.footballhub.comment.dto.CommentDtos.CreateCommentRequest;
import com.footballhub.comment.dto.CommentDtos.UpdateCommentRequest;
import com.footballhub.common.dto.PageResponse;
import com.footballhub.security.AppUserDetails;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/api/v1/blogs/{id}/comments")
    public PageResponse<CommentResponse> list(@PathVariable Long id,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AppUserDetails viewer) {
        return commentService.list(id, sort, page, size, viewer);
    }

    @PostMapping("/api/v1/blogs/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable Long id, @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return commentService.create(id, request, principal);
    }

    @PutMapping("/api/v1/comments/{id}")
    public CommentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return commentService.update(id, request, principal);
    }

    @DeleteMapping("/api/v1/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        commentService.delete(id, principal);
    }
}
