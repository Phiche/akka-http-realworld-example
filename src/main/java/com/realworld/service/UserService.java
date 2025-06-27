package com.realworld.service;

import com.realworld.dto.UserDTO;
import com.realworld.exception.InvalidPasswordException;
import com.realworld.exception.UserAlreadyExistsException;
import com.realworld.model.User;
import com.realworld.repository.UserRepository;
import com.realworld.security.JwtUtils;
// import org.springframework.beans.BeanUtils; // Unused import
import org.springframework.beans.factory.annotation.Autowired; // Optional on constructor
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.realworld.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    // @Autowired is optional here as there's only one constructor
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public UserDTO.UserResponse registerUser(UserDTO.RegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent() || userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("User with given email or username already exists.");
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        try {
            User savedUser = userRepository.save(newUser);
            return convertToUserResponse(savedUser);
        } catch (DataIntegrityViolationException e) {
            // This catch is a fallback, the check above should be primary
            throw new UserAlreadyExistsException("User with given email or username already exists (constraint violation).");
        }
    }

    @Transactional(readOnly = true)
    public UserDTO.UserResponse loginUser(UserDTO.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Invalid password.");
        }

        return convertToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDTO.UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return convertToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDTO.UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return convertToUserResponse(user);
    }


    @Transactional
    public UserDTO.UserResponse updateUser(Long userId, UserDTO.UpdateRequest updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Update fields if provided
        if (updateRequest.getUsername() != null && !updateRequest.getUsername().isBlank()) {
            // Check for username collision if it's being changed to a new one
            if (!user.getUsername().equals(updateRequest.getUsername()) && userRepository.findByUsername(updateRequest.getUsername()).isPresent()) {
                throw new UserAlreadyExistsException("Username " + updateRequest.getUsername() + " is already taken.");
            }
            user.setUsername(updateRequest.getUsername());
        }
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().isBlank()) {
            // Check for email collision if it's being changed to a new one
            if (!user.getEmail().equals(updateRequest.getEmail()) && userRepository.findByEmail(updateRequest.getEmail()).isPresent()) {
                throw new UserAlreadyExistsException("Email " + updateRequest.getEmail() + " is already taken.");
            }
            user.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }
        if (updateRequest.getBio() != null) {
            user.setBio(updateRequest.getBio());
        }
        if (updateRequest.getImage() != null) {
            user.setImage(updateRequest.getImage());
        }
        try {
            User updatedUser = userRepository.save(user);
            return convertToUserResponse(updatedUser);
        } catch (DataIntegrityViolationException e) {
             throw new UserAlreadyExistsException("Update failed due to constraint violation (email/username already taken).");
        }
    }

    private UserDTO.UserResponse convertToUserResponse(User user) {
        String token = jwtUtils.generateToken(user.getUsername(), user.getId());
        return UserDTO.UserResponse.builder()
                .email(user.getEmail())
                .token(token)
                .username(user.getUsername())
                .bio(user.getBio())
                .image(user.getImage())
                .build();
    }

    // Helper method to get User entity by id (used by other services)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    // Helper method to get User entity by username
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}
