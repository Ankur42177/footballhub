package com.footballhub.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import com.footballhub.auth.dto.AuthDtos.LoginRequest;
import com.footballhub.auth.dto.AuthDtos.RegisterRequest;
import com.footballhub.auth.mail.MailService;
import com.footballhub.common.exception.ApiException;
import com.footballhub.config.AppProperties;
import com.footballhub.security.AppUserDetails;
import com.footballhub.user.Role;
import com.footballhub.user.User;
import com.footballhub.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Set<String> RESERVED_USERNAMES = Set.of("admin", "administrator", "moderator", "me",
            "root", "support", "footballhub", "system", "null", "undefined");

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
    private final TokenService tokenService;
    private final MailService mailService;
    private final AppProperties properties;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
            TokenService tokenService, MailService mailService, AppProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.properties = properties;
        this.logoutHandler.setSecurityContextRepository(securityContextRepository);
    }

    @Transactional
    public User register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (RESERVED_USERNAMES.contains(username)) {
            throw ApiException.conflict("USERNAME_TAKEN", "That username is not available.");
        }
        if (users.existsByUsername(username)) {
            throw ApiException.conflict("USERNAME_TAKEN", "That username is already taken.");
        }
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with that email already exists.");
        }
        User user = new User(username, email, passwordEncoder.encode(request.password()),
                request.displayName().trim());
        if (properties.isAdminEmail(email)) {
            user.setRole(Role.ADMIN);
        }
        users.saveAndFlush(user);
        sendVerificationEmail(user);
        return user;
    }

    /** Authenticates with username-or-email and password, then starts a session. */
    public User login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.login().trim(), request.password()));
        }
        catch (DisabledException ex) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account has been suspended.");
        }
        catch (AuthenticationException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Incorrect username, email or password.");
        }
        storeAuthentication(authentication, httpRequest, httpResponse);
        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        return users.findById(principal.getId()).orElseThrow(ApiException::unauthorized);
    }

    /** Starts a session for a user who just registered. */
    public void signIn(User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AppUserDetails principal = AppUserDetails.from(user);
        principal.eraseCredentials();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                principal.getAuthorities());
        storeAuthentication(authentication, httpRequest, httpResponse);
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logoutHandler.logout(httpRequest, httpResponse, authentication);
    }

    /** Always succeeds from the caller's point of view, so it cannot be used to discover accounts. */
    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(User::isEnabled)
                .ifPresent(user -> {
                    String token = tokenService.issue(user, TokenType.RESET_PASSWORD, Duration.ofHours(1));
                    mailService.send(user.getEmail(), "Reset your FootballHub password",
                            "Someone asked to reset your FootballHub password. If it was you, open this link "
                                    + "within an hour:\n\n" + properties.frontendUrl() + "/reset-password?token="
                                    + token + "\n\nIf it wasn't you, you can ignore this email.");
                });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = tokenService.consume(token, TokenType.RESET_PASSWORD);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = tokenService.consume(token, TokenType.VERIFY_EMAIL);
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
        }
    }

    @Transactional
    public void resendVerification(User user) {
        if (user.isEmailVerified()) {
            throw ApiException.conflict("ALREADY_VERIFIED", "Your email address is already verified.");
        }
        sendVerificationEmail(user);
    }

    private void sendVerificationEmail(User user) {
        String token = tokenService.issue(user, TokenType.VERIFY_EMAIL, Duration.ofHours(48));
        mailService.send(user.getEmail(), "Confirm your FootballHub email",
                "Welcome to FootballHub, " + user.getDisplayName() + ".\n\nConfirm your email address within "
                        + "48 hours:\n\n" + properties.frontendUrl() + "/verify-email?token=" + token);
    }

    private void storeAuthentication(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) {
        // Session fixation protection: never reuse a pre-login session id.
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            request.changeSessionId();
        }
        else {
            request.getSession(true);
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
