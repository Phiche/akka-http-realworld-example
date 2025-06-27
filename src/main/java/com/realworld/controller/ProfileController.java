package com.realworld.controller;

import com.realworld.dto.ProfileDTO;
import com.realworld.dto.ProfileDTO;
import com.realworld.model.User;
import com.realworld.service.ProfileService;
import com.realworld.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.realworld.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/profiles/{username}")
public class ProfileController {

    private final ProfileService profileService;
    private final UserService userService; // To fetch User entity from principal if needed

    // @Autowired is optional
    public ProfileController(ProfileService profileService, UserService userService) {
        this.profileService = profileService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ProfileDTO.ProfileResponse> getProfile(@PathVariable String username, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        User currentUser = null;
        if (currentUserDetails != null) {
            currentUser = userService.getUserById(currentUserDetails.getId());
        }
        ProfileDTO.ProfileResponse profileResponse = profileService.getProfile(username, currentUser);
        return ResponseEntity.ok(profileResponse);
    }

    @PostMapping("/follow")
    public ResponseEntity<ProfileDTO.ProfileResponse> followUser(@PathVariable String username, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User currentUser = userService.getUserById(currentUserDetails.getId());
        ProfileDTO.ProfileResponse profileResponse = profileService.followUser(username, currentUser);
        return ResponseEntity.ok(profileResponse);
    }

    @DeleteMapping("/follow")
    public ResponseEntity<ProfileDTO.ProfileResponse> unfollowUser(@PathVariable String username, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User currentUser = userService.getUserById(currentUserDetails.getId());
        ProfileDTO.ProfileResponse profileResponse = profileService.unfollowUser(username, currentUser);
        return ResponseEntity.ok(profileResponse);
    }
}
