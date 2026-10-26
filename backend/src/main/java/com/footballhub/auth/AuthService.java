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
    private final EmailDomainChecker emailDomains;
    private final GoogleTokenVerifier google;

    private static final String GOOGLE_NONCE = "footballhub.googleNonce";
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
            TokenService tokenService, MailService mailService, AppProperties properties,
            EmailDomainChecker emailDomains, GoogleTokenVerifier google) {
        this.emailDomains = emailDomains;
        this.google = google;
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
        if (properties.checkEmailDomains() && !emailDomains.canReceiveMail(email)) {
            throw ApiException.badRequest("EMAIL_DOMAIN_INVALID",
                    "That email address can't receive mail. Check it for typos.");
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
        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        User user = users.findById(principal.getId()).orElseThrow(ApiException::unauthorized);
        if (properties.requireVerifiedEmailToLogin() && !user.isEmailVerified() && user.getRole() != Role.ADMIN) {
            sendVerificationEmail(user);
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                    "Please confirm your email address first. We've sent you a new link.");
        }
        storeAuthentication(authentication, httpRequest, httpResponse);
        return user;
    }

    /**
     * Starts a Google sign-in: a one-time nonce kept in this browser's session. Google puts it
     * inside the ID token, so a token obtained anywhere else can't be replayed here.
     */
    public java.util.Map<String, String> googleSignInStart(HttpServletRequest httpRequest) {
        if (!google.enabled()) {
            throw ApiException.notFound("Google sign-in");
        }
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String nonce = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        httpRequest.getSession(true).setAttribute(GOOGLE_NONCE, nonce);
        return java.util.Map.of("clientId", properties.google().clientId(), "nonce", nonce);
    }

    /** Signs in with a verified Google ID token: an existing account by Google ID or email, or a new one. */
    @Transactional
    public User loginWithGoogle(String credential, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        HttpSession session = httpRequest.getSession(false);
        String nonce = session == null ? null : (String) session.getAttribute(GOOGLE_NONCE);
        if (session != null) {
            session.removeAttribute(GOOGLE_NONCE);
        }
        GoogleTokenVerifier.GoogleIdentity identity = google.verify(credential, nonce);

        User user = users.findByGoogleSubject(identity.subject())
                .or(() -> users.findByEmail(identity.email()))
                .orElseGet(() -> newGoogleUser(identity));
        if (user.getGoogleSubject() == null) {
            user.setGoogleSubject(identity.subject());
        }
        else if (!user.getGoogleSubject().equals(identity.subject())) {
            throw new ApiException(HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_MISMATCH",
                    "This email is linked to a different Google account.");
        }
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account has been suspended.");
        }
        if (!user.isEmailVerified()) {
            user.setEmailVerifiedAt(java.time.Instant.now());
        }
        if (properties.isAdminEmail(user.getEmail()) && user.getRole() != Role.ADMIN) {
            user.setRole(Role.ADMIN);
        }
        users.saveAndFlush(user);
        signIn(user, httpRequest, httpResponse);
        return user;
    }

    private User newGoogleUser(GoogleTokenVerifier.GoogleIdentity identity) {
        String base = identity.email().substring(0, identity.email().indexOf('@'))
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
        base = base.replaceAll("^_|_$", "");
        if (base.length() < 3) {
            base = "fan_" + base;
        }
        base = base.length() > 24 ? base.substring(0, 24) : base;
        String username = base;
        for (int attempt = 0; RESERVED_USERNAMES.contains(username) || users.existsByUsername(username); attempt++) {
            username = base + "_" + (attempt < 5 ? String.valueOf(attempt + 2) : Integer.toString(RANDOM.nextInt(100000)));
        }
        String name = identity.name() == null || identity.name().isBlank() ? base : identity.name().trim();
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        // Unusable random password: the account signs in with Google (or resets a password later).
        User user = new User(username, identity.email(),
                passwordEncoder.encode(java.util.Base64.getEncoder().encodeToString(secret)),
                name.length() > 80 ? name.substring(0, 80) : name);
        user.setEmailVerifiedAt(java.time.Instant.now());
        if (properties.isAdminEmail(identity.email())) {
            user.setRole(Role.ADMIN);
        }
        return users.saveAndFlush(user);
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
