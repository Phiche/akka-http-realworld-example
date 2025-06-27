package com.realworld.service;

import com.realworld.dto.TagDTO;
import com.realworld.model.Tag;
import com.realworld.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList; // Added import
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private Set<String> tagNames;
    private Set<Tag> existingTags;

    @BeforeEach
    void setUp() {
        tagNames = new HashSet<>(Arrays.asList("java", "spring", "test"));

        existingTags = new HashSet<>();
        existingTags.add(Tag.builder().id(1L).name("java").build());
        existingTags.add(Tag.builder().id(2L).name("spring").build());
    }

    @Test
    void getAllTags_success() {
        List<String> allTagNamesList = Arrays.asList("java", "spring", "test", "docker");
        when(tagRepository.findAllTagNames()).thenReturn(allTagNamesList);

        TagDTO.MultipleTagsResponse response = tagService.getAllTags();

        assertNotNull(response);
        assertEquals(4, response.getTags().size());
        assertTrue(response.getTags().containsAll(allTagNamesList));
        verify(tagRepository, times(1)).findAllTagNames();
    }

    @Test
    void findOrCreateTags_allNew() {
        Set<String> newTagNames = new HashSet<>(Arrays.asList("docker", "kubernetes"));
        // Return a mutable set for existingTags, even if empty
        when(tagRepository.findByNameIn(newTagNames)).thenReturn(new HashSet<>());

        List<Tag> savedTags = newTagNames.stream().map(name -> Tag.builder().name(name).build()).collect(Collectors.toList());
        // Simulate IDs being set upon save
        savedTags.get(0).setId(10L);
        savedTags.get(1).setId(11L);

        when(tagRepository.saveAll(anySet())).thenAnswer(invocation -> {
            Set<Tag> tagsToSave = invocation.getArgument(0);
            // This is a simplified mock, real saveAll returns List<S>
            // For this test, we need to return a list of tags that would have been saved.
            // The key is that the input to saveAll are the new tags.
            return tagsToSave.stream().map(t -> {
                if (t.getName().equals("docker")) t.setId(10L);
                if (t.getName().equals("kubernetes")) t.setId(11L);
                return t;
            }).collect(Collectors.toList());
        });


        Set<Tag> result = tagService.findOrCreateTags(newTagNames);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(tag -> "docker".equals(tag.getName()) && tag.getId() != null));
        assertTrue(result.stream().anyMatch(tag -> "kubernetes".equals(tag.getName())&& tag.getId() != null));
        verify(tagRepository, times(1)).findByNameIn(newTagNames);
        verify(tagRepository, times(1)).saveAll(anySet());
    }

    @Test
    void findOrCreateTags_allExist() {
        Set<String> existingNamesOnly = new HashSet<>(Arrays.asList("java", "spring"));
        when(tagRepository.findByNameIn(existingNamesOnly)).thenReturn(existingTags);

        Set<Tag> result = tagService.findOrCreateTags(existingNamesOnly);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(existingTags));
        verify(tagRepository, times(1)).findByNameIn(existingNamesOnly);
        verify(tagRepository, never()).saveAll(anySet());
    }

    @Test
    void findOrCreateTags_mixedNewAndExisting() {
        // tagNames = {"java", "spring", "test"}
        // existingTags = {"java", "spring"}
        // "test" is new
        when(tagRepository.findByNameIn(tagNames)).thenReturn(existingTags); // Returns existing "java", "spring"

        Tag newTag = Tag.builder().name("test").build();
        // Simulate saveAll for the new tag "test"
        when(tagRepository.saveAll(anySet())).thenAnswer(invocation -> {
            Set<Tag> tagsToSave = invocation.getArgument(0);
            // In this specific call, tagsToSave should contain only the Tag("test")
            return tagsToSave.stream().map(t -> {
                if(t.getName().equals("test")) t.setId(3L); // Assign ID to new tag
                return t;
            }).collect(Collectors.toList());
        });

        Set<Tag> result = tagService.findOrCreateTags(tagNames);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(tag -> "test".equals(tag.getName()) && tag.getId() == 3L));
        assertTrue(result.stream().anyMatch(tag -> "java".equals(tag.getName())));
        verify(tagRepository, times(1)).findByNameIn(tagNames);
        verify(tagRepository, times(1)).saveAll(argThat(iterable -> {
            List<Tag> list = new ArrayList<>();
            iterable.forEach(list::add);
            return list.size() == 1 && list.stream().anyMatch(t -> "test".equals(t.getName()));
        }));
    }

    @Test
    void findOrCreateTags_emptyInput() {
        Set<Tag> result = tagService.findOrCreateTags(Collections.emptySet());
        assertTrue(result.isEmpty());
        verify(tagRepository, never()).findByNameIn(anySet());
        verify(tagRepository, never()).saveAll(anySet());
    }

    @Test
    void findOrCreateTags_nullInput() {
        Set<Tag> result = tagService.findOrCreateTags(null);
        assertTrue(result.isEmpty());
         verify(tagRepository, never()).findByNameIn(anySet());
        verify(tagRepository, never()).saveAll(anySet());
    }


    @Test
    void getTagsByNames_success() {
        when(tagRepository.findByNameIn(tagNames)).thenReturn(existingTags);
        Set<Tag> result = tagService.getTagsByNames(tagNames);
        assertEquals(existingTags, result);
        verify(tagRepository, times(1)).findByNameIn(tagNames);
    }

    @Test
    void getTagsByNames_emptyInput() {
        Set<Tag> result = tagService.getTagsByNames(Collections.emptySet());
        assertTrue(result.isEmpty());
        verify(tagRepository, never()).findByNameIn(anySet());
    }

    @Test
    void getTagsByNames_nullInput() {
        Set<Tag> result = tagService.getTagsByNames(null);
        assertTrue(result.isEmpty());
        verify(tagRepository, never()).findByNameIn(anySet());
    }
}
