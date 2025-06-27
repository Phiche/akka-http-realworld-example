package com.realworld.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class TagDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MultipleTagsResponse {
        private List<String> tags;
    }
}
