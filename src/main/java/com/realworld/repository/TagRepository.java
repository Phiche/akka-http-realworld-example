package com.realworld.repository;

import com.realworld.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    Set<Tag> findByNameIn(Set<String> names);

    // Get all tag names
    @Query("SELECT t.name FROM Tag t")
    List<String> findAllTagNames();

    // Find tags associated with a specific article slug
    @Query("SELECT t FROM Tag t JOIN t.articles a WHERE a.slug = :slug")
    Set<Tag> findByArticleSlug(@Param("slug") String slug);
}
