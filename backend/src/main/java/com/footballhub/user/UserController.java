package com.footballhub.user;

import com.footballhub.security.AppUserDetails;
import com.footballhub.user.dto.UserDtos.ChangePasswordRequest;
import com.footballhub.user.dto.UserDtos.MeResponse;
import com.footballhub.user.dto.UserDtos.PublicProfileResponse;
import com.footballhub.user.dto.UserDtos.UpdateProfileRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/v1/users/me")
    public MeResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return userService.me(principal);
    }

    @PatchMapping("/api/v1/users/me")
    public MeResponse updateProfile(@AuthenticationPrincipal AppUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal, request);
    }

    @PostMapping("/api/v1/users/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AppUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal, request);
    }

    @PostMapping("/api/v1/users/me/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(@AuthenticationPrincipal AppUserDetails principal) {
        userService.resendVerification(principal);
    }

    @GetMapping("/api/v1/profiles/{username}")
    public PublicProfileResponse profile(@PathVariable String username) {
        return userService.publicProfile(username);
    }
}
