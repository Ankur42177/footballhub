package com.footballhub.blog;

import com.footballhub.blog.dto.BlogDtos.BlogDetailResponse;
import com.footballhub.blog.dto.BlogDtos.BlogSummaryResponse;
import com.footballhub.blog.dto.BlogDtos.CreateBlogRequest;
import com.footballhub.blog.dto.BlogDtos.UpdateBlogRequest;
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
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    /** Public feed: search (q), club slug, author username, sort (latest|top|trending|oldest), paging. */
    @GetMapping("/api/v1/blogs")
    public PageResponse<BlogSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String club,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AppUserDetails viewer) {
        return blogService.feed(q, club, author, sort, page, size, viewer);
    }

    @GetMapping("/api/v1/blogs/{slug}")
    public BlogDetailResponse get(@PathVariable String slug, @AuthenticationPrincipal AppUserDetails viewer) {
        return blogService.getBySlug(slug, viewer);
    }

    @PostMapping("/api/v1/blogs")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogDetailResponse create(@Valid @RequestBody CreateBlogRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return blogService.create(request, principal);
    }

    @PutMapping("/api/v1/blogs/{id}")
    public BlogDetailResponse update(@PathVariable Long id, @Valid @RequestBody UpdateBlogRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return blogService.update(id, request, principal);
    }

    @DeleteMapping("/api/v1/blogs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        blogService.delete(id, principal);
    }

    @PostMapping("/api/v1/blogs/{id}/publish")
    public BlogDetailResponse publish(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return blogService.publish(id, principal);
    }

    @PostMapping("/api/v1/blogs/{id}/archive")
    public BlogDetailResponse archive(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return blogService.archive(id, principal);
    }

    /** The signed-in author's own posts, including drafts. Optional status filter. */
    @GetMapping("/api/v1/users/me/blogs")
    public PageResponse<BlogSummaryResponse> mine(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AppUserDetails principal) {
        return blogService.myBlogs(principal, status, page, size);
    }
}
