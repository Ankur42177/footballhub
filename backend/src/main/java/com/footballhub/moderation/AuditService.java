package com.footballhub.moderation;

import com.footballhub.user.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records important administrative and moderation actions in the same transaction. */
@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogs;

    public AuditService(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    public void record(User actor, String action, String targetType, Long targetId, String details) {
        String safeDetails = details == null || details.length() <= 1000 ? details : details.substring(0, 997) + "...";
        auditLogs.save(new AuditLog(actor, action, targetType, targetId, safeDetails));
    }
}
