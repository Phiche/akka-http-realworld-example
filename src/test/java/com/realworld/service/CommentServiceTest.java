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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository; // Used for author's 'following' status in response

    @InjectMocks
    private CommentService commentService;

    private User author;
    private User viewer;
    private Article article;
    private Comment comment;
    private CommentDTO.CreateCommentRequest createCommentRequest;

    @BeforeEach
    void setUp() {
        author = User.builder().id(1L).username("author").bio("Author bio").image("author.png").build();
        viewer = User.builder().id(2L).username("viewer").build(); // Represents the currentUser

        article = Article.builder().id(10L).slug("test-article").author(author).build();

        comment = Comment.builder()
                .id(100L)
                .body("Test comment body")
                .article(article)
                .author(author)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();

        createCommentRequest = CommentDTO.CreateCommentRequest.builder().body("New comment").build();
    }

    @Test
    void addCommentToArticle_success() {
        when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(101L); // Simulate ID assignment
            c.setCreatedAt(OffsetDateTime.now());
            c.setUpdatedAt(OffsetDateTime.now());
            return c;
        });
        // For author profile in response (author is viewing their own comment's author profile)
        // when(userRepository.isFollowing(author.getId(), author.getId())).thenReturn(false); // This is not called if author == currentUser

        CommentDTO.CommentResponse response = commentService.addCommentToArticle("test-article", createCommentRequest, author);

        assertNotNull(response);
        assertEquals("New comment", response.getBody());
        assertEquals(author.getUsername(), response.getAuthor().getUsername());
        assertFalse(response.getAuthor().isFollowing()); // Author of comment is not followed by author (self)
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void addCommentToArticle_articleNotFound() {
        when(articleRepository.findBySlug("unknown-slug")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> commentService.addCommentToArticle("unknown-slug", createCommentRequest, author));
    }

    @Test
    void getCommentsByArticleSlug_success() {
        when(articleRepository.existsBySlug("test-article")).thenReturn(true);
        when(commentRepository.findByArticleSlugOrderByCreatedAtDesc("test-article")).thenReturn(Collections.singletonList(comment));
        // Assume 'viewer' is getting comments, and 'viewer' is not following 'author'
        when(userRepository.isFollowing(viewer.getId(), author.getId())).thenReturn(false);

        CommentDTO.MultipleCommentsResponse response = commentService.getCommentsByArticleSlug("test-article", viewer);

        assertNotNull(response);
        assertEquals(1, response.getComments().size());
        CommentDTO.CommentResponse commentResponse = response.getComments().get(0);
        assertEquals(comment.getBody(), commentResponse.getBody());
        assertEquals(author.getUsername(), commentResponse.getAuthor().getUsername());
        assertFalse(commentResponse.getAuthor().isFollowing());
    }

    @Test
    void getCommentsByArticleSlug_success_viewerIsFollowingAuthor() {
        when(articleRepository.existsBySlug("test-article")).thenReturn(true);
        when(commentRepository.findByArticleSlugOrderByCreatedAtDesc("test-article")).thenReturn(Collections.singletonList(comment));
        // Assume 'viewer' is getting comments, and 'viewer' IS following 'author'
        when(userRepository.isFollowing(viewer.getId(), author.getId())).thenReturn(true);

        CommentDTO.MultipleCommentsResponse response = commentService.getCommentsByArticleSlug("test-article", viewer);

        CommentDTO.CommentResponse commentResponse = response.getComments().get(0);
        assertTrue(commentResponse.getAuthor().isFollowing());
    }

    @Test
    void getCommentsByArticleSlug_success_noCurrentUser() {
        when(articleRepository.existsBySlug("test-article")).thenReturn(true);
        when(commentRepository.findByArticleSlugOrderByCreatedAtDesc("test-article")).thenReturn(Collections.singletonList(comment));
        // No current user, so isFollowing should not be called for the author.

        CommentDTO.MultipleCommentsResponse response = commentService.getCommentsByArticleSlug("test-article", null);

        CommentDTO.CommentResponse commentResponse = response.getComments().get(0);
        assertFalse(commentResponse.getAuthor().isFollowing()); // Default to false if no current user
        verify(userRepository, never()).isFollowing(anyLong(), anyLong());
    }


    @Test
    void getCommentsByArticleSlug_articleNotFound() {
        when(articleRepository.existsBySlug("unknown-slug")).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> commentService.getCommentsByArticleSlug("unknown-slug", viewer));
    }

    @Test
    void deleteComment_success() {
        when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        // 'author' is deleting their own comment
        doNothing().when(commentRepository).delete(comment);

        assertDoesNotThrow(() -> commentService.deleteComment("test-article", comment.getId(), author));
        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    void deleteComment_articleNotFound() {
        when(articleRepository.findBySlug("unknown-slug")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> commentService.deleteComment("unknown-slug", comment.getId(), author));
    }

    @Test
    void deleteComment_commentNotFound() {
        when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> commentService.deleteComment("test-article", 999L, author));
    }

    @Test
    void deleteComment_commentDoesNotBelongToArticle() {
        Article otherArticle = Article.builder().id(99L).slug("other-article").build();
        comment.setArticle(otherArticle); // Comment belongs to a different article

        when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article)); // Trying to delete via "test-article" slug
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThrows(ResourceNotFoundException.class, () -> commentService.deleteComment("test-article", comment.getId(), author));
    }


    @Test
    void deleteComment_unauthorized() {
        User anotherUser = User.builder().id(3L).username("anotherUser").build();
        when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        // 'anotherUser' tries to delete 'author's comment
        assertThrows(SecurityException.class, () -> commentService.deleteComment("test-article", comment.getId(), anotherUser));
    }
}
