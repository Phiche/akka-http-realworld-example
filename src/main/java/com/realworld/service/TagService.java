package com.realworld.service;

import com.realworld.dto.TagDTO;
import com.realworld.model.Tag;
import com.realworld.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TagService {

    private final TagRepository tagRepository;

    // @Autowired is optional
    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public TagDTO.MultipleTagsResponse getAllTags() {
        List<String> tagNames = tagRepository.findAllTagNames();
        return new TagDTO.MultipleTagsResponse(tagNames);
    }

    /**
     * Finds existing tags or creates new ones.
     *
     * @param tagNames Set of tag names.
     * @return Set of managed Tag entities.
     */
    @Transactional
    public Set<Tag> findOrCreateTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<Tag> existingTags = tagRepository.findByNameIn(tagNames);
        Set<String> existingTagNames = existingTags.stream().map(Tag::getName).collect(Collectors.toSet());

        Set<Tag> newTagsToCreate = tagNames.stream()
                .filter(name -> !existingTagNames.contains(name))
                .map(Tag::new) // Assumes Tag constructor Tag(String name)
                .collect(Collectors.toSet());

        if (!newTagsToCreate.isEmpty()) {
            List<Tag> savedNewTags = tagRepository.saveAll(newTagsToCreate);
            existingTags.addAll(savedNewTags);
        }

        return existingTags;
    }

    @Transactional(readOnly = true)
    public Set<Tag> getTagsByNames(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }
        return tagRepository.findByNameIn(tagNames);
    }
}
