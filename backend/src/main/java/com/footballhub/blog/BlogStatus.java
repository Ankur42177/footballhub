package com.footballhub.blog;

public enum BlogStatus {
    DRAFT,
    /** Submitted by a non-admin author; hidden from the public until a reviewer approves it. */
    PENDING_REVIEW,
    PUBLISHED,
    /** Not approved by the reviewer; the author can edit and submit again. */
    REJECTED,
    ARCHIVED
}
