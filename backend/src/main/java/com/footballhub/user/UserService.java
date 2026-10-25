package com.footballhub.user;

import java.util.Locale;

import com.footballhub.auth.AuthService;
import com.footballhub.common.exception.ApiException;
import com.footballhub.security.AppUserDetails;
import com.footballhub.security.CurrentUser;
import com.footballhub.user.dto.UserDtos.ChangePasswordRequest;
import com.footballhub.user.dto.UserDtos.MeResponse;
import com.footballhub.user.dto.UserDtos.PublicProfileResponse;
import com.footballhub.user.dto.UserDtos.UpdateProfileRequest;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository users;
    private final CurrentUser currentUser;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final NamedParameterJdbcTemplate jdbc;

    public UserService(UserRepository users, CurrentUser currentUser, PasswordEncoder passwordEncoder,
            AuthService authService, NamedParameterJdbcTemplate jdbc) {
        this.users = users;
        this.currentUser = currentUser;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public MeResponse me(AppUserDetails principal) {
        return MeResponse.from(currentUser.require(principal));
    }

    public MeResponse updateProfile(AppUserDetails principal, UpdateProfileRequest request) {
        User user = currentUser.require(principal);
        user.setDisplayName(request.displayName().trim());
        String bio = request.bio() == null ? null : request.bio().trim();
        user.setBio(bio == null || bio.isEmpty() ? null : bio);
        return MeResponse.from(user);
    }

    public void changePassword(AppUserDetails principal, ChangePasswordRequest request) {
        User user = currentUser.require(principal);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("INVALID_PASSWORD", "Your current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    public void resendVerification(AppUserDetails principal) {
        authService.resendVerification(currentUser.require(principal));
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse publicProfile(String username) {
        User user = users.findByUsername(username.toLowerCase(Locale.ROOT))
                .filter(User::isEnabled)
                .orElseThrow(() -> ApiException.notFound("Profile"));
        long[] stats = jdbc.queryForObject("""
                SELECT COUNT(*) AS posts, COALESCE(SUM(vote_count), 0) AS votes
                FROM blogs
                WHERE author_id = :authorId AND status = 'PUBLISHED' AND hidden = FALSE
                """, new MapSqlParameterSource("authorId", user.getId()),
                (rs, rowNum) -> new long[] { rs.getLong("posts"), rs.getLong("votes") });
        return new PublicProfileResponse(user.getUsername(), user.getDisplayName(), user.getBio(), user.getRole(),
                user.getCreatedAt(), stats == null ? 0 : stats[0], stats == null ? 0 : stats[1]);
    }
}
