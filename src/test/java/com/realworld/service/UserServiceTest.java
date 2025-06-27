package com.realworld.service;

import com.realworld.dto.UserDTO;
import com.realworld.exception.InvalidPasswordException;
import com.realworld.exception.UserAlreadyExistsException;
import com.realworld.exception.ResourceNotFoundException;
import com.realworld.model.User;
import com.realworld.repository.UserRepository;
import com.realworld.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDTO.RegistrationRequest registrationRequest;
    private UserDTO.LoginRequest loginRequest;
    private UserDTO.UpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .bio("Test bio")
                .image("test.png")
                .build();

        registrationRequest = UserDTO.RegistrationRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .build();

        loginRequest = UserDTO.LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        updateRequest = UserDTO.UpdateRequest.builder()
                .username("updateduser")
                .email("updated@example.com")
                .bio("Updated bio")
                .build();
    }

    @Test
    void registerUser_success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registrationRequest.getPassword())).thenReturn("encodedNewPassword");

        User savedUser = User.builder()
            .id(2L)
            .username(registrationRequest.getUsername())
            .email(registrationRequest.getEmail())
            .password("encodedNewPassword")
            .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtils.generateToken(savedUser.getUsername(), savedUser.getId())).thenReturn("test-token");

        UserDTO.UserResponse response = userService.registerUser(registrationRequest);

        assertNotNull(response);
        assertEquals(registrationRequest.getUsername(), response.getUsername());
        assertEquals(registrationRequest.getEmail(), response.getEmail());
        assertEquals("test-token", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_emailExists() {
        when(userRepository.findByEmail(registrationRequest.getEmail())).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(registrationRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_usernameExists() {
        when(userRepository.findByEmail(registrationRequest.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(registrationRequest.getUsername())).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(registrationRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_dataIntegrityViolation() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registrationRequest.getPassword())).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenThrow(DataIntegrityViolationException.class);

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(registrationRequest));
    }


    @Test
    void loginUser_success() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtUtils.generateToken(user.getUsername(), user.getId())).thenReturn("test-token");

        UserDTO.UserResponse response = userService.loginUser(loginRequest);

        assertNotNull(response);
        assertEquals(user.getUsername(), response.getUsername());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals("test-token", response.getToken());
    }

    @Test
    void loginUser_userNotFound() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.loginUser(loginRequest));
    }

    @Test
    void loginUser_invalidPassword() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(false);
        assertThrows(InvalidPasswordException.class, () -> userService.loginUser(loginRequest));
    }

    @Test
    void getCurrentUser_byId_success() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(user.getUsername(), user.getId())).thenReturn("test-token");

        UserDTO.UserResponse response = userService.getCurrentUser(user.getId());
        assertNotNull(response);
        assertEquals(user.getUsername(), response.getUsername());
    }

    @Test
    void getCurrentUser_byUsername_success() {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(user.getUsername(), user.getId())).thenReturn("test-token");

        UserDTO.UserResponse response = userService.getCurrentUser(user.getUsername());
        assertNotNull(response);
        assertEquals(user.getUsername(), response.getUsername());
    }


    @Test
    void getCurrentUser_notFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getCurrentUser(user.getId()));
    }

    @Test
    void updateUser_success() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByUsername(updateRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(updateRequest.getEmail())).thenReturn(Optional.empty());

        // The user object 'user' will be modified by the service before save is called.
        // So, when save is called, user.getUsername() will be updateRequest.getUsername().
        User updatedUser = User.builder()
                .id(user.getId())
                .username(updateRequest.getUsername()) // important for token generation
                .email(updateRequest.getEmail())
                .password(user.getPassword()) // password not changing in this test case
                .bio(updateRequest.getBio())
                .image(user.getImage()) // image not changing
                .build();

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        // Stubbing for jwtUtils should use the username that will be present after the update
        when(jwtUtils.generateToken(updateRequest.getUsername(), user.getId())).thenReturn("test-token");

        // Simulate password encoding if password is part of update
        // if (updateRequest.getPassword() != null) {
        //    when(passwordEncoder.encode(updateRequest.getPassword())).thenReturn("newEncodedPassword");
        // }


        UserDTO.UserResponse response = userService.updateUser(user.getId(), updateRequest);

        assertNotNull(response);
        assertEquals(updateRequest.getUsername(), response.getUsername());
        assertEquals(updateRequest.getEmail(), response.getEmail());
        assertEquals(updateRequest.getBio(), response.getBio());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_usernameTaken() {
        User existingUserWithUsername = User.builder().id(3L).username(updateRequest.getUsername()).build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user)); // Current user
        when(userRepository.findByUsername(updateRequest.getUsername())).thenReturn(Optional.of(existingUserWithUsername));

        assertThrows(UserAlreadyExistsException.class, () -> userService.updateUser(user.getId(), updateRequest));
    }

    @Test
    void updateUser_emailTaken() {
        User existingUserWithEmail = User.builder().id(3L).email(updateRequest.getEmail()).build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user)); // Current user
        when(userRepository.findByUsername(updateRequest.getUsername())).thenReturn(Optional.empty()); // Assume username is fine
        when(userRepository.findByEmail(updateRequest.getEmail())).thenReturn(Optional.of(existingUserWithEmail));

        assertThrows(UserAlreadyExistsException.class, () -> userService.updateUser(user.getId(), updateRequest));
    }

    @Test
    void updateUser_passwordChange() {
        UserDTO.UpdateRequest passwordUpdateRequest = UserDTO.UpdateRequest.builder()
                .password("newPassword123")
                .build();
        User userToUpdate = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("oldEncodedPassword") // Initial password
                .build();

        when(userRepository.findById(userToUpdate.getId())).thenReturn(Optional.of(userToUpdate));
        when(passwordEncoder.encode(passwordUpdateRequest.getPassword())).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0)); // return the same user passed to save
        when(jwtUtils.generateToken(anyString(), anyLong())).thenReturn("test-token");

        userService.updateUser(userToUpdate.getId(), passwordUpdateRequest);

        verify(passwordEncoder, times(1)).encode("newPassword123");
        verify(userRepository, times(1)).save(argThat(savedUser -> savedUser.getPassword().equals("newEncodedPassword")));
    }


    @Test
    void updateUser_userNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(user.getId(), updateRequest));
    }
}
