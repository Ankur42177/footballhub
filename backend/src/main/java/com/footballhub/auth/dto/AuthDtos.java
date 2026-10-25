package com.footballhub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request bodies for the authentication endpoints. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 30)
            @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Use letters, numbers and underscores only.")
            String username,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 128, message = "Use at least 8 characters.") String password,
            @NotBlank @Size(max = 80) String displayName) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 255) String login,
            @NotBlank @Size(max = 128) String password) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 255) String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(max = 200) String token,
            @NotBlank @Size(min = 8, max = 128, message = "Use at least 8 characters.") String newPassword) {
    }

    public record VerifyEmailRequest(@NotBlank @Size(max = 200) String token) {
    }
}
