package com.realworld.repository;

import com.realworld.model.Article;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    Optional<Article> findBySlug(String slug);

    boolean existsBySlug(String slug); // Added this method

    // Find articles by author's username
    List<Article> findByAuthorUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    // Find articles favorited by a user (identified by username)
    @Query("SELECT a FROM Article a JOIN a.favoritedBy u WHERE u.username = :username ORDER BY a.createdAt DESC")
    List<Article> findFavoritedByUsername(@Param("username") String username, Pageable pageable);

    // Find articles by tag name
    @Query("SELECT a FROM Article a JOIN a.tags t WHERE t.name = :tagName ORDER BY a.createdAt DESC")
    List<Article> findByTagName(@Param("tagName") String tagName, Pageable pageable);

    // Find articles for a user's feed (articles by authors the user follows)
    @Query("SELECT a FROM Article a " +
           "JOIN a.author author " +
           "JOIN author.followers follower " +
           "WHERE follower.id = :userId " +
           "ORDER BY a.createdAt DESC")
    List<Article> findFeedArticles(@Param("userId") Long userId, Pageable pageable);


    // Fetch article with author and tags for detailed view
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.tags WHERE a.slug = :slug")
    Optional<Article> findBySlugWithAuthorAndTags(@Param("slug") String slug);

    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.tags WHERE a.id = :id")
    Optional<Article> findByIdWithAuthorAndTags(@Param("id") Long id);

    // Fetch article with its favoritedBy users
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.favoritedBy WHERE a.slug = :slug")
    Optional<Article> findBySlugWithFavoritedBy(@Param("slug") String slug);

    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.favoritedBy WHERE a.id = :id")
    Optional<Article> findByIdWithFavoritedBy(@Param("id") Long id);


    // Get all articles with author, tags, and favoritedBy information (potentially heavy, use with pagination)
    // This is a complex fetch, might be better to fetch ids first then details if performance is an issue.
    // For specifications, it's often better to fetch IDs only first, then fetch entities by ID.
    // However, JpaSpecificationExecutor works on the root entity.
    // This query is an example for a comprehensive list, but specific use cases might need more tailored queries.

    // For listing articles with eager fetching of collections for Specifications
    // This is generally not recommended with Specifications directly due to potential N+1 or Cartesian product issues
    // if not handled carefully. A common approach is to fetch IDs via Specification,
    // then fetch the entities with their collections by these IDs.
    // However, if you must, ensure distinct and proper joins.

    // Query to check if a user has favorited an article
    @Query("SELECT CASE WHEN COUNT(fav) > 0 THEN true ELSE false END " +
           "FROM Article a JOIN a.favoritedBy fav WHERE a.id = :articleId AND fav.id = :userId")
    boolean isFavoritedBy(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Query("SELECT COUNT(u) FROM Article a JOIN a.favoritedBy u WHERE a.id = :articleId")
    long countFavorites(@Param("articleId") Long articleId);

    // Get multiple articles by their IDs, with author and tags.
    // Useful after a specification query that returns only IDs.
    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.tags WHERE a.id IN :ids ORDER BY a.createdAt DESC")
    List<Article> findByIdInWithAuthorAndTags(@Param("ids") Set<Long> ids);
}
