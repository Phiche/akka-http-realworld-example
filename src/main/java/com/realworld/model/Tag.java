package com.realworld.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "articles") // Avoid issues with bidirectional relationships in generated methods
@Entity
@Table(name = "tags", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name")
})
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToMany(mappedBy = "tags")
    @lombok.ToString.Exclude // Already excluded in @EqualsAndHashCode via class-level
    private Set<Article> articles = new HashSet<>();

    public Tag(String name) {
        this.name = name;
    }
}
