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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    // private final UserService userService; // Unused dependency
    private final TagService tagService;

    // @Autowired is optional
    public ArticleService(ArticleRepository articleRepository, UserRepository userRepository,
                          /* UserService userService, */ TagService tagService) { // UserService removed
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        // this.userService = userService;
        this.tagService = tagService;
    }

    @Transactional
    public ArticleDTO.ArticleResponse createArticle(ArticleDTO.CreateArticleRequest request, User author) {
        Set<Tag> tags = tagService.findOrCreateTags(request.getTagList());
        String slug = SlugUtil.toSlug(request.getTitle());

        // Ensure slug uniqueness (simple retry mechanism, or more robust approach if high contention)
        int attempt = 0;
        String currentSlug = slug;
        while (articleRepository.findBySlug(currentSlug).isPresent()) {
            attempt++;
            currentSlug = slug + "-" + attempt;
        }
        slug = currentSlug;

        Article article = Article.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .body(request.getBody())
                .slug(slug)
                .author(author)
                .tags(new HashSet<>(tags)) // Use a mutable copy
                .comments(new HashSet<>())
                .favoritedBy(new HashSet<>())
                .build();

        Article savedArticle = articleRepository.save(article);
        // Author is the current user in this context. A new article isn't favorited by default by its author.
        return convertToArticleResponse(savedArticle, author);
    }


    @Transactional(readOnly = true)
    public ArticleDTO.ArticleResponse getArticle(String slug, User currentUser) { // currentUser can be null
        Article article = articleRepository.findBySlugWithAuthorAndTags(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        return convertToArticleResponse(article, currentUser);
    }

    @Transactional
    public ArticleDTO.ArticleResponse updateArticle(String slug, ArticleDTO.UpdateArticleRequest request, User currentUser) {
        Article article = articleRepository.findBySlugWithAuthorAndTags(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        if (!article.getAuthor().getId().equals(currentUser.getId())) {
            throw new SecurityException("User not authorized to update this article."); // Or specific exception
        }

        boolean titleChanged = false;
        if (request.getTitle() != null && !request.getTitle().isBlank() && !article.getTitle().equals(request.getTitle())) {
            article.setTitle(request.getTitle());
            titleChanged = true;
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            article.setDescription(request.getDescription());
        }
        if (request.getBody() != null && !request.getBody().isBlank()) {
            article.setBody(request.getBody());
        }

        if (titleChanged) {
            String newSlug = SlugUtil.toSlug(article.getTitle());
            int attempt = 0;
            String currentSlug = newSlug;
            // Ensure new slug is unique if it changed, or unique if it's different from old one
            while (articleRepository.findBySlug(currentSlug).filter(a -> !a.getId().equals(article.getId())).isPresent()) {
                attempt++;
                currentSlug = newSlug + "-" + attempt;
            }
            article.setSlug(currentSlug);
        }

        Article updatedArticle = articleRepository.save(article);
        return convertToArticleResponse(updatedArticle, currentUser);
    }

    @Transactional
    public void deleteArticle(String slug, User currentUser) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        if (!article.getAuthor().getId().equals(currentUser.getId())) {
            throw new SecurityException("User not authorized to delete this article.");
        }
        articleRepository.delete(article);
    }

    @Transactional(readOnly = true)
    public ArticleDTO.MultipleArticlesResponse listArticles(ArticleDTO.ArticleQueryParameters params, User currentUser) {
        Pageable pageable = PageRequest.of(params.getOffset() / params.getLimit(), params.getLimit(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Article> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (params.getTag() != null && !params.getTag().isEmpty()) {
                predicates.add(cb.equal(root.join("tags").get("name"), params.getTag()));
            }
            if (params.getAuthor() != null && !params.getAuthor().isEmpty()) {
                predicates.add(cb.equal(root.join("author").get("username"), params.getAuthor()));
            }
            if (params.getFavoritedBy() != null && !params.getFavoritedBy().isEmpty()) {
                User favoritingUser = userRepository.findByUsername(params.getFavoritedBy())
                        .orElse(null); // Or throw if favoritedBy user must exist
                if (favoritingUser != null) {
                    predicates.add(cb.isMember(favoritingUser, root.get("favoritedBy")));
                } else {
                    // If favoritedBy user doesn't exist, effectively no articles match
                    predicates.add(cb.disjunction()); // or cb.isFalse(cb.literal(true))
                }
            }
            // Ensure query returns distinct articles when joining with collections like tags or favoritedBy
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Article> articles = articleRepository.findAll(spec, pageable).getContent();
        long totalArticles = articleRepository.count(spec); // For accurate count with filters

        List<ArticleDTO.ArticleResponse> articleResponses = articles.stream()
                .map(article -> {
                    boolean favorited = false;
                    if (currentUser != null) {
                        favorited = articleRepository.isFavoritedBy(article.getId(), currentUser.getId());
                    }
                    long favoritesCount = articleRepository.countFavorites(article.getId());
                    // Author and Tags should be fetched efficiently due to @EntityGraph or join fetch in repo if needed,
                    // or ensure they are loaded before this map operation.
                    // The current findAll(spec, pageable) won't eagerly load collections well.
                    // A common pattern is to fetch IDs with spec, then fetch full entities by ID.
                    // For simplicity here, we'll assume they might be loaded or rely on individual queries in convert.
                    // This part might need optimization for N+1 issues.
                    // Let's fetch them explicitly for now.
                    Article fullArticle = articleRepository.findByIdWithAuthorAndTags(article.getId()).orElse(article);
                    // Use the convertToArticleResponse that takes currentUser to correctly set author's following status
                    return convertToArticleResponse(fullArticle, currentUser);
                })
                .collect(Collectors.toList());

        return new ArticleDTO.MultipleArticlesResponse(articleResponses, (int) totalArticles);
    }

    @Transactional(readOnly = true)
    public ArticleDTO.MultipleArticlesResponse getFeedArticles(User currentUser, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Article> articles = articleRepository.findFeedArticles(currentUser.getId(), pageable);

        List<ArticleDTO.ArticleResponse> articleResponses = articles.stream()
                .map(article -> {
                    // Assuming findFeedArticles already fetches author and tags or we fetch them here
                     Article fullArticle = articleRepository.findByIdWithAuthorAndTags(article.getId()).orElse(article);
                    // Use the convertToArticleResponse that takes currentUser
                    return convertToArticleResponse(fullArticle, currentUser);
                })
                .collect(Collectors.toList());

        return new ArticleDTO.MultipleArticlesResponse(articleResponses, articleResponses.size());
    }


    @Transactional
    public ArticleDTO.ArticleResponse favoriteArticle(String slug, User currentUser) {
        Article article = articleRepository.findBySlugWithAuthorAndTags(slug) // Fetch with author/tags for response
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + slug));

        User user = userRepository.findByIdWithFavoriteArticles(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found for favorite operation."));

        user.getFavoriteArticles().add(article);
        userRepository.save(user);

        // Pass currentUser to correctly determine author's following status for the response
        return convertToArticleResponse(article, currentUser);
    }

    @Transactional
    public ArticleDTO.ArticleResponse unfavoriteArticle(String slug, User currentUser) {
        Article article = articleRepository.findBySlugWithAuthorAndTags(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + slug));

        User user = userRepository.findByIdWithFavoriteArticles(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found for unfavorite operation."));

        user.getFavoriteArticles().remove(article);
        userRepository.save(user);

        return convertToArticleResponse(article, currentUser);
    }

    // Internal helper to build the response DTO
    private ArticleDTO.ArticleResponse buildArticleResponse(Article article, boolean isFavorited, long favoritesCount, User authorEntity, User viewer) {
        boolean authorFollowingByViewer = false;
        if (viewer != null && authorEntity != null && !viewer.getId().equals(authorEntity.getId())) {
            authorFollowingByViewer = userRepository.isFollowing(viewer.getId(), authorEntity.getId());
        }

        ProfileDTO.ProfileResponse authorProfile = ProfileDTO.ProfileResponse.builder()
                .username(authorEntity.getUsername())
                .bio(authorEntity.getBio())
                .image(authorEntity.getImage())
                .following(authorFollowingByViewer)
                .build();

        return ArticleDTO.ArticleResponse.builder()
                .slug(article.getSlug())
                .title(article.getTitle())
                .description(article.getDescription())
                .body(article.getBody())
                .tagList(article.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .favorited(isFavorited)
                .favoritesCount(favoritesCount)
                .author(authorProfile)
                .build();
    }

    // Public converter method, determines favorite status and author's following status based on the current user
    public ArticleDTO.ArticleResponse convertToArticleResponse(Article article, User currentUser) {
        boolean isFavoritedByCurrentUser = false;
        if (currentUser != null) {
            isFavoritedByCurrentUser = articleRepository.isFavoritedBy(article.getId(), currentUser.getId());
        }
        long favoritesCount = articleRepository.countFavorites(article.getId());

        // Ensure article.getAuthor() is not null and is fetched
        User author = (article.getAuthor() != null) ? article.getAuthor() :
                      articleRepository.findByIdWithAuthorAndTags(article.getId())
                                     .map(Article::getAuthor)
                                     .orElseThrow(() -> new ResourceNotFoundException("Article author not found for article id: " + article.getId()));

        return buildArticleResponse(article, isFavoritedByCurrentUser, favoritesCount, author, currentUser);
    }

    // Overload for when favorite status and count are already known (e.g. after creation)
    // Note: This was the original structure, but the one above (taking only article and current user) is generally safer
    // to ensure all data is consistently fetched. Keeping this might be useful for specific optimized paths if needed.
    // For now, let's standardize on the (Article, User) signature for public conversion.
    // The create method can call the (Article, User) version too.
    /*
    private ArticleDTO.ArticleResponse convertToArticleResponse(Article article, boolean favorited, long favoritesCount, User authorEntity) {
         // This version implicitly assumes viewer is null or not relevant for author's following status
        return buildArticleResponse(article, favorited, favoritesCount, authorEntity, null);
    }
    */
}
