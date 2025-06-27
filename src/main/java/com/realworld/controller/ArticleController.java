package com.realworld.controller;

import com.realworld.dto.ArticleDTO;
import com.realworld.dto.ArticleDTO;
import com.realworld.model.User;
import com.realworld.service.ArticleService;
import com.realworld.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.realworld.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final UserService userService; // To fetch User entity from principal

    // @Autowired is optional
    public ArticleController(ArticleService articleService, UserService userService) {
        this.articleService = articleService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ArticleDTO.ArticleResponse> createArticle(@Valid @RequestBody ArticleDTO.CreateArticleRequest request, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User author = userService.getUserById(currentUserDetails.getId());
        ArticleDTO.ArticleResponse articleResponse = articleService.createArticle(request, author);
        return ResponseEntity.status(HttpStatus.CREATED).body(articleResponse);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ArticleDTO.ArticleResponse> getArticle(@PathVariable String slug, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        User currentUser = null;
        if (currentUserDetails != null) {
            currentUser = userService.getUserById(currentUserDetails.getId());
        }
        ArticleDTO.ArticleResponse articleResponse = articleService.getArticle(slug, currentUser);
        return ResponseEntity.ok(articleResponse);
    }

    @PutMapping("/{slug}")
    public ResponseEntity<ArticleDTO.ArticleResponse> updateArticle(@PathVariable String slug, @Valid @RequestBody ArticleDTO.UpdateArticleRequest request, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User currentUser = userService.getUserById(currentUserDetails.getId());
        ArticleDTO.ArticleResponse articleResponse = articleService.updateArticle(slug, request, currentUser);
        return ResponseEntity.ok(articleResponse);
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteArticle(@PathVariable String slug, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User currentUser = userService.getUserById(currentUserDetails.getId());
        articleService.deleteArticle(slug, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ArticleDTO.MultipleArticlesResponse> listArticles(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String favorited, // favorited by username
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {

        ArticleDTO.ArticleQueryParameters params = ArticleDTO.ArticleQueryParameters.builder()
                .tag(tag).author(author).favoritedBy(favorited)
                .limit(limit).offset(offset).build();

        User currentUser = null;
        if (currentUserDetails != null) {
            currentUser = userService.getUserById(currentUserDetails.getId());
        }

        ArticleDTO.MultipleArticlesResponse response = articleService.listArticles(params, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/feed")
    public ResponseEntity<ArticleDTO.MultipleArticlesResponse> getFeedArticles(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User currentUser = userService.getUserById(currentUserDetails.getId());
        ArticleDTO.MultipleArticlesResponse response = articleService.getFeedArticles(currentUser, limit, offset);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/favorite")
    public ResponseEntity<ArticleDTO.ArticleResponse> favoriteArticle(@PathVariable String slug, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User currentUser = userService.getUserById(currentUserDetails.getId());
        ArticleDTO.ArticleResponse articleResponse = articleService.favoriteArticle(slug, currentUser);
        return ResponseEntity.ok(articleResponse);
    }

    @DeleteMapping("/{slug}/favorite")
    public ResponseEntity<ArticleDTO.ArticleResponse> unfavoriteArticle(@PathVariable String slug, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User currentUser = userService.getUserById(currentUserDetails.getId());
        ArticleDTO.ArticleResponse articleResponse = articleService.unfavoriteArticle(slug, currentUser);
        return ResponseEntity.ok(articleResponse);
    }
}
