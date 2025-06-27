package com.realworld.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public class ArticleDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeName("article")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    public static class ArticleResponse {
        private String slug;
        private String title;
        private String description;
        private String body;
        private List<String> tagList; // Changed from Set to List for defined order if any
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private OffsetDateTime createdAt;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private OffsetDateTime updatedAt;
        private boolean favorited;
        private long favoritesCount;
        private ProfileDTO.ProfileResponse author;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MultipleArticlesResponse {
        private List<ArticleResponse> articles;
        private int articlesCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeName("article")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    public static class CreateArticleRequest {
        @NotBlank(message = "Title cannot be blank")
        private String title;
        @NotBlank(message = "Description cannot be blank")
        private String description;
        @NotBlank(message = "Body cannot be blank")
        private String body;
        private Set<String> tagList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeName("article")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    public static class UpdateArticleRequest {
        private String title;
        private String description;
        private String body;
        // Tag list update is not explicitly mentioned in typical Realworld PUT /api/articles/{slug}
        // but if it were, it would be here. Usually, tags are not updated via this request.
    }

    // This can be used as a parameter object for service methods
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleQueryParameters {
        private String tag;
        private String author; // author username
        private String favoritedBy; // username of user who favorited
        private int limit = 20;
        private int offset = 0;
    }
}
