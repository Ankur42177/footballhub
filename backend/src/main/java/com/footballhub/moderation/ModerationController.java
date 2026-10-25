package com.footballhub.moderation;

import com.footballhub.common.dto.PageResponse;
import com.footballhub.moderation.dto.ModerationDtos.AdminUserResponse;
import com.footballhub.moderation.dto.ModerationDtos.AuditLogResponse;
import com.footballhub.moderation.dto.ModerationDtos.ChangeRoleRequest;
import com.footballhub.moderation.dto.ModerationDtos.ChangeStatusRequest;
import com.footballhub.moderation.dto.ModerationDtos.CreateReportRequest;
import com.footballhub.moderation.dto.ModerationDtos.ModerationResult;
import com.footballhub.moderation.dto.ModerationDtos.ReportCreatedResponse;
import com.footballhub.moderation.dto.ModerationDtos.ReportResponse;
import com.footballhub.moderation.dto.ModerationDtos.ResolveReportRequest;
import com.footballhub.security.AppUserDetails;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping("/api/v1/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportCreatedResponse report(@Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return moderationService.createReport(request, principal);
    }

    /** status: open (default), all, OPEN, REVIEWING, RESOLVED or DISMISSED. */
    @GetMapping("/api/v1/admin/reports")
    public PageResponse<ReportResponse> reports(@RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AppUserDetails principal) {
        return moderationService.listReports(status, page, size, principal);
    }

    @PostMapping("/api/v1/admin/reports/{id}/resolve")
    public ReportResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveReportRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return moderationService.resolveReport(id, request, principal);
    }

    @PostMapping("/api/v1/admin/blogs/{id}/hide")
    public ModerationResult hideBlog(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return moderationService.hideBlog(id, true, principal);
    }

    @PostMapping("/api/v1/admin/blogs/{id}/unhide")
    public ModerationResult unhideBlog(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return moderationService.hideBlog(id, false, principal);
    }

    @PostMapping("/api/v1/admin/comments/{id}/hide")
    public ModerationResult hideComment(@PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal) {
        return moderationService.hideComment(id, true, principal);
    }

    @PostMapping("/api/v1/admin/comments/{id}/unhide")
    public ModerationResult unhideComment(@PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal) {
        return moderationService.hideComment(id, false, principal);
    }

    @GetMapping("/api/v1/admin/users")
    public PageResponse<AdminUserResponse> users(@RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AppUserDetails principal) {
        return moderationService.listUsers(q, page, size, principal);
    }

    @PatchMapping("/api/v1/admin/users/{id}/role")
    public AdminUserResponse changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return moderationService.changeRole(id, request.role(), principal);
    }

    @PatchMapping("/api/v1/admin/users/{id}/status")
    public AdminUserResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return moderationService.changeStatus(id, request.enabled(), principal);
    }

    @GetMapping("/api/v1/admin/audit-logs")
    public PageResponse<AuditLogResponse> auditLog(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @AuthenticationPrincipal AppUserDetails principal) {
        return moderationService.auditLog(page, size, principal);
    }
}
