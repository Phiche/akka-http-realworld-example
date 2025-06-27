package com.realworld.service;

import com.realworld.dto.CommentDTO;
import com.realworld.dto.ProfileDTO;
import com.realworld.exception.ResourceNotFoundException;
import com.realworld.model.Article;
import com.realworld.model.Comment;
import com.realworld.model.User;
import com.realworld.repository.ArticleRepository;
import com.realworld.repository.CommentRepository;
import com.realworld.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository; // For author's following status

    // @Autowired is optional
    public CommentService(CommentRepository commentRepository, ArticleRepository articleRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommentDTO.CommentResponse addCommentToArticle(String slug, CommentDTO.CreateCommentRequest request, User author) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Comment comment = Comment.builder()
                .body(request.getBody())
                .article(article)
                .author(author)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return convertToCommentResponse(savedComment, author); // Passing 'author' as the potential viewer for their own comment's author profile
    }

    @Transactional(readOnly = true)
    public CommentDTO.MultipleCommentsResponse getCommentsByArticleSlug(String slug, User currentUser) {
        if (!articleRepository.existsBySlug(slug)) {
            throw new ResourceNotFoundException("Article not found with slug: " + slug);
        }
        List<Comment> comments = commentRepository.findByArticleSlugOrderByCreatedAtDesc(slug);
        List<CommentDTO.CommentResponse> commentResponses = comments.stream()
                .map(comment -> convertToCommentResponse(comment, currentUser))
                .collect(Collectors.toList());
        return new CommentDTO.MultipleCommentsResponse(commentResponses);
    }

    @Transactional
    public void deleteComment(String slug, Long commentId, User currentUser) {
        // Ensure article exists, though not strictly necessary for deleting comment by ID if comment ID is globally unique
        // However, the slug is part of the API path, implying context.
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getArticle().getId().equals(article.getId())) {
             throw new ResourceNotFoundException("Comment does not belong to the specified article.");
        }

        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new SecurityException("User not authorized to delete this comment."); // Or specific HttpStatusException
        }

        commentRepository.delete(comment);
    }


    private CommentDTO.CommentResponse convertToCommentResponse(Comment comment, User currentUser) {
        User commentAuthor = comment.getAuthor(); // Assuming author is fetched with comment or fetch it
        if (commentAuthor == null && comment.getAuthor() != null) { // Defensive check if LAZY loaded
             commentAuthor = userRepository.findById(comment.getAuthor().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment author not found"));
        } else if (commentAuthor == null) {
             throw new IllegalStateException("Comment author is null and cannot be fetched.");
        }


        boolean followingAuthor = false;
        if (currentUser != null && !currentUser.getId().equals(commentAuthor.getId())) {
            followingAuthor = userRepository.isFollowing(currentUser.getId(), commentAuthor.getId());
        }

        ProfileDTO.ProfileResponse authorProfile = ProfileDTO.ProfileResponse.builder()
                .username(commentAuthor.getUsername())
                .bio(commentAuthor.getBio())
                .image(commentAuthor.getImage())
                .following(followingAuthor)
                .build();

        return CommentDTO.CommentResponse.builder()
                .id(comment.getId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .body(comment.getBody())
                .author(authorProfile)
                .build();
    }
}
