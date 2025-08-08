package com.realworld.article;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ArticleController {
    @GetMapping("/articles")
    public List<Article> all() {
        return List.of(new Article("Hello from Spring Boot"));
    }
}
