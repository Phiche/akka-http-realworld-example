package com.realworld.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 1024)
    private String bio;

    private String image;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "author")
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private Set<Article> articles;

    @OneToMany(mappedBy = "author")
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private Set<Comment> comments;

    @ManyToMany
    @JoinTable(
            name = "followers",
            joinColumns = @JoinColumn(name = "followee_id"), // user being followed
            inverseJoinColumns = @JoinColumn(name = "user_id")  // user who is following
    )
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private Set<User> followers; // People who follow this user

    @ManyToMany(mappedBy = "followers")
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private Set<User> following; // People this user follows

    @ManyToMany
    @JoinTable(
            name = "article_favorites",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "article_id")
    )
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private Set<Article> favoriteArticles;
}
