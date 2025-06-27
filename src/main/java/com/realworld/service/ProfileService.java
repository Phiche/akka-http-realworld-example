package com.realworld.service;

import com.realworld.dto.ProfileDTO;
import com.realworld.exception.ResourceNotFoundException;
import com.realworld.model.User;
import com.realworld.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    // private final UserService userService; // This dependency is not used

    // @Autowired is optional
    public ProfileService(UserRepository userRepository) { // Removed UserService from constructor
        this.userRepository = userRepository;
        // this.userService = userService;
    }

    @Transactional(readOnly = true)
    public ProfileDTO.ProfileResponse getProfile(String username, User currentUser) { // currentUser can be null for anonymous access
        User profileUser = userRepository.findByUsernameWithFollowers(username) // Fetch profile user with their list of followers
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for username: " + username));

        boolean following = false;
        if (currentUser != null) {
            // To check if currentUser is following profileUser,
            // we see if currentUser exists in profileUser's list of followers.
            following = profileUser.getFollowers().stream()
                    .anyMatch(follower -> follower.getId().equals(currentUser.getId()));
        }
        return convertToProfileResponse(profileUser, following);
    }


    @Transactional
    public ProfileDTO.ProfileResponse followUser(String usernameToFollow, User actualCurrentUser) {
        if (actualCurrentUser.getUsername().equals(usernameToFollow)) {
            throw new IllegalArgumentException("User cannot follow themselves.");
        }

        // Fetch current user with their 'following' collection to safely modify it
        User currentUser = userRepository.findByIdWithFollowing(actualCurrentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found. This should not happen."));


        User userToFollow = userRepository.findByUsername(usernameToFollow) // No need for followers here
                .orElseThrow(() -> new ResourceNotFoundException("User to follow not found: " + usernameToFollow));

        currentUser.getFollowing().add(userToFollow);
        userRepository.save(currentUser); // Save the owning side

        return convertToProfileResponse(userToFollow, true);
    }

    @Transactional
    public ProfileDTO.ProfileResponse unfollowUser(String usernameToUnfollow, User actualCurrentUser) {
        // Fetch current user with their 'following' collection
        User currentUser = userRepository.findByIdWithFollowing(actualCurrentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found. This should not happen."));

        User userToUnfollow = userRepository.findByUsername(usernameToUnfollow)
                .orElseThrow(() -> new ResourceNotFoundException("User to unfollow not found: " + usernameToUnfollow));

        currentUser.getFollowing().remove(userToUnfollow);
        userRepository.save(currentUser);

        return convertToProfileResponse(userToUnfollow, false);
    }


    private ProfileDTO.ProfileResponse convertToProfileResponse(User user, boolean following) {
        return ProfileDTO.ProfileResponse.builder()
                .username(user.getUsername())
                .bio(user.getBio())
                .image(user.getImage())
                .following(following)
                .build();
    }
}
