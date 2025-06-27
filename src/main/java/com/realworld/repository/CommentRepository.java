package com.realworld.repository;

import com.realworld.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Find comments by article slug, ordered by creation date
    @Query("SELECT c FROM Comment c JOIN c.article a WHERE a.slug = :slug ORDER BY c.createdAt DESC")
    List<Comment> findByArticleSlugOrderByCreatedAtDesc(@Param("slug") String slug);

    // Find comments by article ID, ordered by creation date
    List<Comment> findByArticleIdOrderByCreatedAtDesc(Long articleId);

    // Fetch a comment with its author and article (useful for specific comment operations)
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.author LEFT JOIN FETCH c.article WHERE c.id = :id")
    Optional<Comment> findByIdWithAuthorAndArticle(@Param("id") Long id);
}
