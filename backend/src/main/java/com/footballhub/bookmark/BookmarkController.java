package com.footballhub.bookmark;

import com.footballhub.blog.dto.BlogDtos.BlogSummaryResponse;
import com.footballhub.bookmark.BookmarkService.BookmarkResponse;
import com.footballhub.common.dto.PageResponse;
import com.footballhub.security.AppUserDetails;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/api/v1/blogs/{id}/bookmark")
    public BookmarkResponse add(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return bookmarkService.add(id, principal);
    }

    @DeleteMapping("/api/v1/blogs/{id}/bookmark")
    public BookmarkResponse remove(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return bookmarkService.remove(id, principal);
    }

    @GetMapping("/api/v1/users/me/bookmarks")
    public PageResponse<BlogSummaryResponse> list(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @AuthenticationPrincipal AppUserDetails principal) {
        return bookmarkService.list(principal, page, size);
    }
}
