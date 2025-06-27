package com.realworld.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProfileDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeName("profile")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    public static class ProfileResponse {
        private String username;
        private String bio;
        private String image;
        private boolean following;
    }
}
