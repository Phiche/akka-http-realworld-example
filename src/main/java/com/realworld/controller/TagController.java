package com.realworld.controller;

import com.realworld.dto.TagDTO;
import com.realworld.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    // @Autowired is optional
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ResponseEntity<TagDTO.MultipleTagsResponse> getAllTags() {
        TagDTO.MultipleTagsResponse tagsResponse = tagService.getAllTags();
        return ResponseEntity.ok(tagsResponse);
    }
}
