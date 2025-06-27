package com.realworld.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "articles", uniqueConstraints = {
        @UniqueConstraint(columnNames = "slug")
})
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false)
    private String description;

    @Lob // For TEXT type
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private Set<Comment> comments = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "article_tags",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany(mappedBy = "favoriteArticles")
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private Set<User> favoritedBy = new HashSet<>();

    // Helper methods for managing bidirectional relationships if needed
    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getArticles().add(this);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getArticles().remove(this);
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setArticle(this);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setArticle(null);
    }
}
