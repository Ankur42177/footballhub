package com.footballhub.notification;

import java.util.Map;

import com.footballhub.common.dto.PageResponse;
import com.footballhub.notification.NotificationService.NotificationResponse;
import com.footballhub.security.AppUserDetails;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/api/v1/notifications")
    public PageResponse<NotificationResponse> list(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @AuthenticationPrincipal AppUserDetails principal) {
        return notificationService.list(principal, page, size);
    }

    @GetMapping("/api/v1/notifications/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AppUserDetails principal) {
        return notificationService.unreadCount(principal);
    }

    @PostMapping("/api/v1/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal AppUserDetails principal) {
        notificationService.markAllRead(principal);
    }
}
