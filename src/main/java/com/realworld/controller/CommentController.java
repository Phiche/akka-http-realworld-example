package com.realworld.controller;

import com.realworld.dto.CommentDTO;
import com.realworld.dto.CommentDTO;
import com.realworld.model.User;
import com.realworld.service.CommentService;
import com.realworld.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.realworld.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/articles/{slug}/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService; // To fetch User entity from principal

    // @Autowired is optional
    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<CommentDTO.CommentResponse> addComment(
            @PathVariable String slug,
            @Valid @RequestBody CommentDTO.CreateCommentRequest request, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User author = userService.getUserById(currentUserDetails.getId());
        CommentDTO.CommentResponse commentResponse = commentService.addCommentToArticle(slug, request, author);
        return ResponseEntity.status(HttpStatus.CREATED).body(commentResponse);
    }

    @GetMapping
    public ResponseEntity<CommentDTO.MultipleCommentsResponse> getComments(
            @PathVariable String slug, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        User currentUser = null;
        if (currentUserDetails != null) {
            currentUser = userService.getUserById(currentUserDetails.getId());
        }
        CommentDTO.MultipleCommentsResponse commentsResponse = commentService.getCommentsByArticleSlug(slug, currentUser);
        return ResponseEntity.ok(commentsResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String slug,
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        if (currentUserDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User currentUser = userService.getUserById(currentUserDetails.getId());
        commentService.deleteComment(slug, id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
