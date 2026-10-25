package com.footballhub.notification;

import java.time.Instant;
import java.util.Map;

import com.footballhub.common.dto.PageResponse;
import com.footballhub.common.util.Pagination;
import com.footballhub.security.AppUserDetails;
import com.footballhub.security.CurrentUser;
import com.footballhub.user.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    public record NotificationResponse(Long id, NotificationType type, String message, String link, boolean read,
            Instant createdAt) {
    }

    private final NotificationRepository notifications;
    private final CurrentUser currentUser;

    public NotificationService(NotificationRepository notifications, CurrentUser currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    /** Records a notification, skipping self-notifications. */
    public void notify(User recipient, User actor, NotificationType type, String message, String link) {
        if (recipient == null || (actor != null && recipient.getId().equals(actor.getId()))) {
            return;
        }
        String safeMessage = message.length() > 300 ? message.substring(0, 297) + "..." : message;
        notifications.save(new Notification(recipient, type, safeMessage, link));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(AppUserDetails principal, Integer page, Integer size) {
        User user = currentUser.require(principal);
        Page<Notification> result = notifications.findByRecipientId(user.getId(),
                Pagination.of(page, size, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return PageResponse.of(result, result.getContent().stream()
                .map(n -> new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getLink(),
                        n.getReadAt() != null, n.getCreatedAt()))
                .toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> unreadCount(AppUserDetails principal) {
        User user = currentUser.require(principal);
        return Map.of("count", notifications.countByRecipientIdAndReadAtIsNull(user.getId()));
    }

    public void markAllRead(AppUserDetails principal) {
        User user = currentUser.require(principal);
        notifications.markAllRead(user.getId(), Instant.now());
    }
}
