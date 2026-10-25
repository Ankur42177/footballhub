package com.footballhub.user;

public enum Role {
    USER,
    MODERATOR,
    ADMIN;

    public boolean isStaff() {
        return this == MODERATOR || this == ADMIN;
    }
}
