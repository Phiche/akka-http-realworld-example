package com.realworld.service;

import com.realworld.dto.ArticleDTO;
import com.realworld.dto.ProfileDTO;
import com.realworld.exception.ResourceNotFoundException;
import com.realworld.model.Article;
import com.realworld.model.Tag;
import com.realworld.model.User;
import com.realworld.repository.ArticleRepository;
import com.realworld.repository.UserRepository;
import com.realworld.util.SlugUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor; // Added import
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService; // Though not directly used, it's a constructor dep.

    @Mock
    private TagService tagService;

    // @Spy // If we wanted to spy on SlugUtil static methods (needs PowerMockito or similar for static)
    // For unit tests, we usually assume utility classes like SlugUtil work correctly or test them separately.
    // We can mock its behavior if it were an instance, or predict its output.

    @InjectMocks
    private ArticleService articleService;

    private User author;
    private User viewer;
    private Article article;
    private ArticleDTO.CreateArticleRequest createArticleRequest;
    private ArticleDTO.UpdateArticleRequest updateArticleRequest;
    private Set<Tag> tags;

    @BeforeEach
    void setUp() {
        author = User.builder().id(1L).username("author").bio("Author Bio").image("author.png").favoriteArticles(new HashSet<>()).build();
        viewer = User.builder().id(2L).username("viewer").favoriteArticles(new HashSet<>()).build();

        tags = new HashSet<>(Arrays.asList(Tag.builder().name("java").build(), Tag.builder().name("spring").build()));

        article = Article.builder()
                .id(10L)
                .slug("test-title") // SlugUtil.toSlug("Test Title") might be "test-title-xxxxxx"
                .title("Test Title")
                .description("Test description")
                .body("Test body")
                .author(author)
                .tags(tags)
                .favoritedBy(new HashSet<>())
                .comments(new HashSet<>())
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();

        // Re-adjust slug to match expected SlugUtil output for "Test Title"
        article.setSlug(SlugUtil.toSlugSimple("Test Title") + "-mockedSuffix");


        createArticleRequest = ArticleDTO.CreateArticleRequest.builder()
                .title("New Article Title")
                .description("New desc")
                .body("New body")
                .tagList(new HashSet<>(Arrays.asList("tag1", "tag2")))
                .build();

        updateArticleRequest = ArticleDTO.UpdateArticleRequest.builder()
                .title("Updated Title")
                .description("Updated desc")
                .body("Updated body")
                .build();
    }

    @Test
    void createArticle_success() {
        String expectedSlug = SlugUtil.toSlugSimple(createArticleRequest.getTitle()); // Base part
        when(tagService.findOrCreateTags(createArticleRequest.getTagList())).thenReturn(tags);
        when(articleRepository.findBySlug(anyString())).thenReturn(Optional.empty()); // No slug collision
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            a.setId(11L); // Simulate save
            // Ensure the slug in 'a' contains the base part
            assertTrue(a.getSlug().startsWith(expectedSlug));
            return a;
        });

        // For convertToArticleResponse (when currentUser is the author)
        // isFollowing for author on self is not called by buildArticleResponse.
        // A new article has 0 favorites and is not favorited by author initially.
        when(articleRepository.isFavoritedBy(anyLong(), eq(author.getId()))).thenReturn(false);
        when(articleRepository.countFavorites(anyLong())).thenReturn(0L);


        ArticleDTO.ArticleResponse response = articleService.createArticle(createArticleRequest, author);

        assertNotNull(response);
        assertEquals(createArticleRequest.getTitle(), response.getTitle());
        assertTrue(response.getSlug().startsWith(expectedSlug));
        assertEquals(author.getUsername(), response.getAuthor().getUsername());
        assertFalse(response.isFavorited());
        assertEquals(0, response.getFavoritesCount());
        verify(articleRepository, times(1)).save(any(Article.class));
    }

    @Test
    void createArticle_slugCollisionOnce() {
        String baseSlug = SlugUtil.toSlugSimple(createArticleRequest.getTitle());
        String firstAttemptSlug = baseSlug + "-mockedSuffix1"; // Assume SlugUtil.toSlug generates this
        String secondAttemptSlug = baseSlug + "-1-mockedSuffix2";


        // Mock SlugUtil if it were a bean, or predict its output carefully if static and complex.
        // For this test, we'll assume SlugUtil.toSlug() produces a somewhat predictable pattern,
        // even with random suffix, the retry mechanism adds "-N".
        // Let's assume the first slug generated by SlugUtil.toSlug() is "new-article-title-xxxxxx"
        // We can't easily mock static SlugUtil.toSlug without PowerMock/etc.
        // So, we must rely on its actual behavior and mock repository calls accordingly.
        // The key is that findBySlug will be called multiple times.

        // Let's define the sequence of slugs the service will try:
        // 1. SlugUtil.toSlug(createArticleRequest.getTitle()) -> Let this be "slug-from-util"
        // 2. "slug-from-util-1"
        // For the test, we can't know the exact "xxxxxx" from SlugUtil.
        // So, we use lenient matching for the first call to findBySlug, and then more specific for retry.

        Article existingArticleWithSlug = Article.builder().slug("some-colliding-slug").build();

        when(tagService.findOrCreateTags(createArticleRequest.getTagList())).thenReturn(tags);

        // Use an ArgumentCaptor to capture slugs passed to findBySlug
        ArgumentCaptor<String> slugCaptor = ArgumentCaptor.forClass(String.class);

        // First call to findBySlug (with the slug from SlugUtil) collides
        // Second call (with "-1" suffix) does not.
        when(articleRepository.findBySlug(slugCaptor.capture()))
            .thenReturn(Optional.of(existingArticleWithSlug)) // First call
            .thenReturn(Optional.empty());                  // Second call

        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            a.setId(12L);
            // The saved slug should be the second one tried, i.e., originalSlug + "-1"
            // We can verify this using the captor after the service method is called.
            return a;
        });
        // For convertToArticleResponse (when currentUser is the author)
        when(articleRepository.isFavoritedBy(anyLong(), eq(author.getId()))).thenReturn(false);
        when(articleRepository.countFavorites(anyLong())).thenReturn(0L);


        ArticleDTO.ArticleResponse response = articleService.createArticle(createArticleRequest, author);

        assertNotNull(response);
        List<String> capturedSlugs = slugCaptor.getAllValues();
        assertEquals(2, capturedSlugs.size()); // findBySlug called twice
        String originalSlugFromUtil = capturedSlugs.get(0);
        String retriedSlug = capturedSlugs.get(1);

        assertEquals(originalSlugFromUtil + "-1", retriedSlug);
        assertEquals(retriedSlug, response.getSlug()); // Check it used the suffixed slug

        verify(articleRepository, times(2)).findBySlug(anyString());
        verify(articleRepository, times(1)).save(any(Article.class));
    }


    @Test
    void getArticle_success_notFavorited_viewerNotFollowingAuthor() {
        when(articleRepository.findBySlugWithAuthorAndTags(article.getSlug())).thenReturn(Optional.of(article));
        when(articleRepository.isFavoritedBy(article.getId(), viewer.getId())).thenReturn(false);
        when(articleRepository.countFavorites(article.getId())).thenReturn(0L);
        when(userRepository.isFollowing(viewer.getId(), author.getId())).thenReturn(false);

        ArticleDTO.ArticleResponse response = articleService.getArticle(article.getSlug(), viewer);

        assertNotNull(response);
        assertEquals(article.getTitle(), response.getTitle());
        assertFalse(response.isFavorited());
        assertEquals(0, response.getFavoritesCount());
        assertEquals(author.getUsername(), response.getAuthor().getUsername());
        assertFalse(response.getAuthor().isFollowing());
    }

    @Test
    void getArticle_success_favorited_viewerIsFollowingAuthor() {
        when(articleRepository.findBySlugWithAuthorAndTags(article.getSlug())).thenReturn(Optional.of(article));
        when(articleRepository.isFavoritedBy(article.getId(), viewer.getId())).thenReturn(true);
        when(articleRepository.countFavorites(article.getId())).thenReturn(1L);
        when(userRepository.isFollowing(viewer.getId(), author.getId())).thenReturn(true);

        ArticleDTO.ArticleResponse response = articleService.getArticle(article.getSlug(), viewer);

        assertNotNull(response);
        assertTrue(response.isFavorited());
        assertEquals(1, response.getFavoritesCount());
        assertTrue(response.getAuthor().isFollowing());
    }

    @Test
    void getArticle_success_noCurrentUser() {
        when(articleRepository.findBySlugWithAuthorAndTags(article.getSlug())).thenReturn(Optional.of(article));
        // isFavoritedBy should not be called if currentUser is null
        when(articleRepository.countFavorites(article.getId())).thenReturn(0L);
        // isFollowing should not be called for author if no current user

        ArticleDTO.ArticleResponse response = articleService.getArticle(article.getSlug(), null);

        assertNotNull(response);
        assertFalse(response.isFavorited()); // Default if no current user
        assertEquals(0, response.getFavoritesCount());
        assertEquals(author.getUsername(), response.getAuthor().getUsername());
        assertFalse(response.getAuthor().isFollowing()); // Default if no current user
        verify(articleRepository, never()).isFavoritedBy(anyLong(), anyLong());
        verify(userRepository, never()).isFollowing(anyLong(), anyLong());
    }


    @Test
    void getArticle_notFound() {
        when(articleRepository.findBySlugWithAuthorAndTags("unknown-slug")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> articleService.getArticle("unknown-slug", viewer));
    }

    @Test
    void updateArticle_success() {
        // Assume 'author' is the currentUser trying to update their own article
        when(articleRepository.findBySlugWithAuthorAndTags(article.getSlug())).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // For convertToArticleResponse
        when(articleRepository.isFavoritedBy(article.getId(), author.getId())).thenReturn(false); // Author doesn't favorite own by default
        when(articleRepository.countFavorites(article.getId())).thenReturn(0L);
        // when(userRepository.isFollowing(author.getId(), author.getId())).thenReturn(false); // Not called when viewer is author


        ArticleDTO.ArticleResponse response = articleService.updateArticle(article.getSlug(), updateArticleRequest, author);

        assertNotNull(response);
        assertEquals(updateArticleRequest.getTitle(), response.getTitle());
        assertEquals(updateArticleRequest.getDescription(), article.getDescription()); // Check underlying article
        assertEquals(updateArticleRequest.getBody(), article.getBody());
        verify(articleRepository, times(1)).save(article);
    }

    @Test
    void updateArticle_titleChanged_slugRegenerated_noCollision() {
        when(articleRepository.findBySlugWithAuthorAndTags(article.getSlug())).thenReturn(Optional.of(article));
        // Assume SlugUtil.toSlug(updateArticleRequest.getTitle()) generates a new slug base
        String newSlugBase = SlugUtil.toSlugSimple(updateArticleRequest.getTitle());
        // Mock that this new slug (with its random suffix) doesn't collide
        when(articleRepository.findBySlug(argThat(s -> s.startsWith(newSlugBase)))).thenReturn(Optional.empty());

        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // For convertToArticleResponse (currentUser is author)
        when(articleRepository.isFavoritedBy(anyLong(), eq(author.getId()))).thenReturn(false);
        when(articleRepository.countFavorites(anyLong())).thenReturn(0L);
        // when(userRepository.isFollowing(anyLong(), anyLong())).thenReturn(false); // Not called when viewer is author


        articleService.updateArticle(article.getSlug(), updateArticleRequest, author);

        // Verify the article's slug was updated and starts with the new base
        assertTrue(article.getSlug().startsWith(newSlugBase));
        assertNotEquals(article.getSlug(), SlugUtil.toSlugSimple(article.getTitle())); // Original title's slug base
        verify(articleRepository, times(1)).save(article);
    }


    @Test
    void updateArticle_unauthorized() {
        when(articleRepository.findBySlugWithAuthorAndTags(article.getSlug())).thenReturn(Optional.of(article));
        // 'viewer' tries to update 'author's article
        assertThrows(SecurityException.class, () -> articleService.updateArticle(article.getSlug(), updateArticleRequest, viewer));
    }

    @Test
    void deleteArticle_success() {
        when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
        doNothing().when(articleRepository).delete(article);
        assertDoesNotThrow(() -> articleService.deleteArticle(article.getSlug(), author));
        verify(articleRepository, times(1)).delete(article);
    }

    @Test
    void deleteArticle_unauthorized() {
        when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
        assertThrows(SecurityException.class, () -> articleService.deleteArticle(article.getSlug(), viewer));
    }

    @Test
    void listArticles_noFilters() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Article> articlesList = Collections.singletonList(article);
        Page<Article> articlePage = new PageImpl<>(articlesList, pageable, 1);

        when(articleRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(articlePage);
        when(articleRepository.count(any(Specification.class))).thenReturn(1L);

        // Mocking for convertToArticleResponse inside the loop
        when(articleRepository.findByIdWithAuthorAndTags(article.getId())).thenReturn(Optional.of(article));
        // when(articleRepository.isFavoritedBy(article.getId(), null)).thenReturn(false); // userId for isFavoritedBy cannot be null
        when(articleRepository.countFavorites(article.getId())).thenReturn(0L);
        // Author's following status from perspective of null user is false, and isFollowing won't be called with null viewerId

        ArticleDTO.ArticleQueryParameters params = ArticleDTO.ArticleQueryParameters.builder().limit(20).offset(0).build();
        ArticleDTO.MultipleArticlesResponse response = articleService.listArticles(params, null); // No current user

        assertNotNull(response);
        assertEquals(1, response.getArticlesCount());
        assertEquals(1, response.getArticles().size());
        assertEquals(article.getTitle(), response.getArticles().get(0).getTitle());
        verify(articleRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    // More tests for listArticles with different filters (tag, author, favoritedBy) would be similar,
    // focusing on how the Specification is built or how params are passed.
    // Testing the Specification itself is harder in unit tests.

    @Test
    void getFeedArticles_success() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Article> feedArticlesList = Collections.singletonList(article);
        when(articleRepository.findFeedArticles(viewer.getId(), pageable)).thenReturn(feedArticlesList);

        // Mocking for convertToArticleResponse
        when(articleRepository.findByIdWithAuthorAndTags(article.getId())).thenReturn(Optional.of(article));
        when(articleRepository.isFavoritedBy(article.getId(), viewer.getId())).thenReturn(false);
        when(articleRepository.countFavorites(article.getId())).thenReturn(0L);
        when(userRepository.isFollowing(viewer.getId(), author.getId())).thenReturn(false);


        ArticleDTO.MultipleArticlesResponse response = articleService.getFeedArticles(viewer, 20, 0);

        assertNotNull(response);
        // Feed count is currently size of returned list
        assertEquals(feedArticlesList.size(), response.getArticlesCount());
        assertEquals(1, response.getArticles().size());
        verify(articleRepository, times(1)).findFeedArticles(viewer.getId(), pageable);
    }

    @Test
    void favoriteArticle_success() {
        // viewer favorites article by author
        when(articleRepository.findBySlugWithAuthorAndTags(article.getSlug())).thenReturn(Optional.of(article));
        when(userRepository.findByIdWithFavoriteArticles(viewer.getId())).thenReturn(Optional.of(viewer));
        when(userRepository.save(viewer)).thenReturn(viewer); // viewer's favoriteArticles set is updated

        // For convertToArticleResponse
        // After favoriting, isFavoritedBy should be true for the viewer
        when(articleRepository.isFavoritedBy(article.getId(), viewer.getId())).thenReturn(true);
        when(articleRepository.countFavorites(article.getId())).thenReturn(1L); // Favorites count increases
        when(userRepository.isFollowing(viewer.getId(), author.getId())).thenReturn(false); // Assume not following author


        ArticleDTO.ArticleResponse response = articleService.favoriteArticle(article.getSlug(), viewer);

        assertNotNull(response);
        assertTrue(response.isFavorited());
        assertEquals(1, response.getFavoritesCount());
        assertTrue(viewer.getFavoriteArticles().contains(article));
        verify(userRepository, times(1)).save(viewer);
    }

    @Test
    void favoriteArticle_articleNotFound() {
        when(articleRepository.findBySlugWithAuthorAndTags("unknown-slug")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> articleService.favoriteArticle("unknown-slug", viewer));
    }


    @Test
    void unfavoriteArticle_success() {
        viewer.getFavoriteArticles().add(article); // Pre-condition: viewer has favorited the article
        article.getFavoritedBy().add(viewer); // Simulate bidirectional part for count

        when(articleRepository.findBySlugWithAuthorAndTags(article.getSlug())).thenReturn(Optional.of(article));
        when(userRepository.findByIdWithFavoriteArticles(viewer.getId())).thenReturn(Optional.of(viewer));
        when(userRepository.save(viewer)).thenReturn(viewer);

        // For convertToArticleResponse
        // After unfavoriting, isFavoritedBy should be false
        when(articleRepository.isFavoritedBy(article.getId(), viewer.getId())).thenReturn(false);
        when(articleRepository.countFavorites(article.getId())).thenReturn(0L); // Favorites count decreases
        when(userRepository.isFollowing(viewer.getId(), author.getId())).thenReturn(false);


        ArticleDTO.ArticleResponse response = articleService.unfavoriteArticle(article.getSlug(), viewer);

        assertNotNull(response);
        assertFalse(response.isFavorited());
        assertEquals(0, response.getFavoritesCount());
        assertFalse(viewer.getFavoriteArticles().contains(article));
        verify(userRepository, times(1)).save(viewer);
    }
}
