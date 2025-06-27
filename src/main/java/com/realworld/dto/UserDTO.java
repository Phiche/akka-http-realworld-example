package com.realworld.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeName("user")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    public static class LoginRequest {
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        private String email;

        @NotBlank(message = "Password cannot be blank")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeName("user")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    public static class RegistrationRequest {
        @NotBlank(message = "Username cannot be blank")
        private String username;

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        private String email;

        @NotBlank(message = "Password cannot be blank")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeName("user")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    public static class UpdateRequest {
        private String username;
        @Email(message = "Email should be valid if provided")
        private String email;
        private String password;
        private String bio;
        private String image;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeName("user")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    public static class UserResponse {
        private String email;
        private String token;
        private String username;
        private String bio;
        private String image;
    }
}
