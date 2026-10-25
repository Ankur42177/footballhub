package com.footballhub.moderation;

public enum ReportStatus {
    OPEN,
    REVIEWING,
    RESOLVED,
    DISMISSED;

    public boolean isOpen() {
        return this == OPEN || this == REVIEWING;
    }
}
