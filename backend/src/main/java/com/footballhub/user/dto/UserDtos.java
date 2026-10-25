package com.footballhub.user.dto;

import java.time.Instant;

import com.footballhub.user.Role;
import com.footballhub.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request and response types for the user module. */
public final class UserDtos {

    private UserDtos() {
    }

    /** The signed-in user's own account. Only ever returned to that user. */
    public record MeResponse(Long id, String username, String email, String displayName, String bio, Role role,
            boolean emailVerified, Instant createdAt) {

        public static MeResponse from(User user) {
            return new MeResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(),
                    user.getBio(), user.getRole(), user.isEmailVerified(), user.getCreatedAt());
        }
    }

    /** Public author card used inside blogs and comments. */
    public record AuthorSummary(String username, String displayName) {

        public static AuthorSummary from(User user) {
            return new AuthorSummary(user.getUsername(), user.getDisplayName());
        }
    }

    public record PublicProfileResponse(String username, String displayName, String bio, Role role,
            Instant joinedAt, long publishedPosts, long totalVotes) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 80) String displayName,
            @Size(max = 500) String bio) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword) {
    }
}
