package com.realworld.controller;

import com.realworld.dto.UserDTO;
// import com.realworld.model.User; // Not directly used in controller methods
import com.realworld.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.realworld.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    // @Autowired is optional
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users/login")
    public ResponseEntity<UserDTO.UserResponse> loginUser(@Valid @RequestBody UserDTO.LoginRequest loginRequest) {
        UserDTO.UserResponse userResponse = userService.loginUser(loginRequest);
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/users")
    public ResponseEntity<UserDTO.UserResponse> registerUser(@Valid @RequestBody UserDTO.RegistrationRequest registrationRequest) {
        UserDTO.UserResponse userResponse = userService.registerUser(registrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @GetMapping("/user")
    public ResponseEntity<UserDTO.UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) {
            // This case should be handled by Spring Security config if endpoint is secured and no token provided
            // or token is invalid. The AuthEntryPointJwt would trigger.
            // If a valid token for a non-existent user somehow passes filter, this check is a safeguard.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDTO.UserResponse userResponse = userService.getCurrentUser(currentUserDetails.getId());
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/user")
    public ResponseEntity<UserDTO.UserResponse> updateUser(@Valid @RequestBody UserDTO.UpdateRequest updateRequest, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDTO.UserResponse userResponse = userService.updateUser(currentUserDetails.getId(), updateRequest);
        return ResponseEntity.ok(userResponse);
    }
}
