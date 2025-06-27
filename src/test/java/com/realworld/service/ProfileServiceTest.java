package com.realworld.service;

import com.realworld.dto.ProfileDTO;
import com.realworld.exception.ResourceNotFoundException;
import com.realworld.model.User;
import com.realworld.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    // UserService is a dependency of ProfileService but not directly used in these tests
    // for its own logic, only for fetching User entities if that was the design.
    // However, ProfileService constructor takes UserService.
    // We can mock it if its methods were called, or if constructor needs it.
    // Current ProfileService uses userRepository for all its user fetching needs.
    // Let's update ProfileService constructor if UserService is not strictly needed for its core logic,
    // or provide a mock for UserService if it were.
    // For now, assuming ProfileService can work primarily with UserRepository for its direct needs.
    // Re-checking ProfileService: it does NOT use UserService. It was a thought during design but not implemented.
    // So, no need to mock UserService here if ProfileService doesn't actually use it.
    // Let's verify ProfileService constructor. It takes (UserRepository, UserService).
    // This means we DO need to mock UserService, even if no methods on it are called by ProfileService.
    @Mock
    private UserService userService;


    @InjectMocks
    private ProfileService profileService;

    private User currentUser;
    private User profileUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .username("currentUser")
                .email("current@example.com")
                .following(new HashSet<>()) // Initialize collections
                .followers(new HashSet<>())
                .build();

        profileUser = User.builder()
                .id(2L)
                .username("profileUser")
                .email("profile@example.com")
                .bio("Profile bio")
                .image("profile.png")
                .following(new HashSet<>())
                .followers(new HashSet<>())
                .build();
    }

    @Test
    void getProfile_userExists_currentUserNotFollowing() {
        when(userRepository.findByUsernameWithFollowers("profileUser")).thenReturn(Optional.of(profileUser));
        // currentUser is not in profileUser's followers list

        ProfileDTO.ProfileResponse response = profileService.getProfile("profileUser", currentUser);

        assertNotNull(response);
        assertEquals("profileUser", response.getUsername());
        assertEquals("Profile bio", response.getBio());
        assertFalse(response.isFollowing());
    }

    @Test
    void getProfile_userExists_currentUserIsFollowing() {
        profileUser.getFollowers().add(currentUser); // currentUser is following profileUser
        when(userRepository.findByUsernameWithFollowers("profileUser")).thenReturn(Optional.of(profileUser));

        ProfileDTO.ProfileResponse response = profileService.getProfile("profileUser", currentUser);

        assertNotNull(response);
        assertTrue(response.isFollowing());
    }

    @Test
    void getProfile_userExists_noCurrentUser() {
        when(userRepository.findByUsernameWithFollowers("profileUser")).thenReturn(Optional.of(profileUser));

        ProfileDTO.ProfileResponse response = profileService.getProfile("profileUser", null); // No authenticated user

        assertNotNull(response);
        assertEquals("profileUser", response.getUsername());
        assertFalse(response.isFollowing()); // Should be false if no current user
    }


    @Test
    void getProfile_userNotFound() {
        when(userRepository.findByUsernameWithFollowers("unknownUser")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> profileService.getProfile("unknownUser", currentUser));
    }

    @Test
    void followUser_success() {
        when(userRepository.findByIdWithFollowing(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsername("profileUser")).thenReturn(Optional.of(profileUser));
        when(userRepository.save(any(User.class))).thenReturn(currentUser);

        ProfileDTO.ProfileResponse response = profileService.followUser("profileUser", currentUser);

        assertNotNull(response);
        assertTrue(response.isFollowing()); // The profile returned is of 'profileUser', and 'currentUser' is now following them.
        assertTrue(currentUser.getFollowing().contains(profileUser));
        verify(userRepository, times(1)).save(currentUser);
    }

    @Test
    void followUser_userToFollowNotFound() {
         when(userRepository.findByIdWithFollowing(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsername("unknownUser")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> profileService.followUser("unknownUser", currentUser));
    }

    @Test
    void followUser_currentUserNotFound_shouldNotHappenIfAuthenticated() {
        // This tests if the initial fetch of currentUser (with its 'following' collection) fails.
        // In a real scenario with Spring Security, @AuthenticationPrincipal would provide a valid user or null.
        // If it's null, controller would return 401. If it's not null, user should exist.
        when(userRepository.findByIdWithFollowing(currentUser.getId())).thenReturn(Optional.empty());
        // No need to mock findByUsername for userToFollow as it won't be reached.

        assertThrows(ResourceNotFoundException.class, () -> profileService.followUser("profileUser", currentUser));
    }


    @Test
    void followUser_cannotFollowSelf() {
        // CurrentUser's username is "currentUser"
        assertThrows(IllegalArgumentException.class, () -> profileService.followUser("currentUser", currentUser));
    }


    @Test
    void unfollowUser_success() {
        currentUser.getFollowing().add(profileUser); // currentUser is already following profileUser

        when(userRepository.findByIdWithFollowing(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsername("profileUser")).thenReturn(Optional.of(profileUser));
        when(userRepository.save(any(User.class))).thenReturn(currentUser);


        ProfileDTO.ProfileResponse response = profileService.unfollowUser("profileUser", currentUser);

        assertNotNull(response);
        assertFalse(response.isFollowing());
        assertFalse(currentUser.getFollowing().contains(profileUser));
        verify(userRepository, times(1)).save(currentUser);
    }

    @Test
    void unfollowUser_userToUnfollowNotFound() {
        when(userRepository.findByIdWithFollowing(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsername("unknownUser")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> profileService.unfollowUser("unknownUser", currentUser));
    }
}
