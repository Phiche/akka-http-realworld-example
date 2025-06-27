package com.realworld.repository;

import com.realworld.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    // Check if a user (userId) is following another user (followeeId)
    // This can be inferred from the User entity's relationships, but a direct query might be useful.
    // Alternatively, one could fetch the user and check user.getFollowing().contains(followeeUser)
    // For performance, a dedicated query might be better if this check is frequent and standalone.
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM User u JOIN u.following f WHERE u.id = :userId AND f.id = :followeeId")
    boolean isFollowing(@Param("userId") Long userId, @Param("followeeId") Long followeeId);

    // Get users that the given user (userId) is following
    @Query("SELECT f FROM User u JOIN u.following f WHERE u.id = :userId")
    Set<User> findFollowing(@Param("userId") Long userId);

    // Get users who are following the given user (userId) -> followers
    @Query("SELECT f FROM User u JOIN u.followers f WHERE u.id = :userId")
    Set<User> findFollowers(@Param("userId") Long userId);

    // For fetching user with their followed users (following)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.following WHERE u.id = :userId")
    Optional<User> findByIdWithFollowing(@Param("userId") Long userId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.following WHERE u.username = :username")
    Optional<User> findByUsernameWithFollowing(@Param("username") String username);


    // For fetching user with their followers
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.followers WHERE u.id = :userId")
    Optional<User> findByIdWithFollowers(@Param("userId") Long userId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.followers WHERE u.username = :username")
    Optional<User> findByUsernameWithFollowers(@Param("username") String username);

    // For fetching user with favorite articles
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.favoriteArticles WHERE u.id = :userId")
    Optional<User> findByIdWithFavoriteArticles(@Param("userId") Long userId);

     @Query("SELECT u FROM User u LEFT JOIN FETCH u.favoriteArticles WHERE u.username = :username")
    Optional<User> findByUsernameWithFavoriteArticles(@Param("username") String username);

}
