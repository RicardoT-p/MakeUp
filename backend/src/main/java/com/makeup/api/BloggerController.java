package com.makeup.api;

import com.makeup.model.Blogger;
import com.makeup.service.BloggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "美妆博主档案")
@RestController
@RequestMapping("/api/v1/bloggers")
public class BloggerController {

    private final BloggerService bloggerService;

    public BloggerController(BloggerService bloggerService) {
        this.bloggerService = bloggerService;
    }

    @Operation(summary = "列出启用的博主档案")
    @GetMapping
    public List<Blogger> findAll() { return bloggerService.findAll(); }

    @Operation(summary = "读取博主档案")
    @GetMapping("/{id}")
    public Blogger find(@PathVariable long id) { return bloggerService.find(id); }

    @Operation(summary = "新增博主档案")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Blogger create(@Valid @RequestBody Blogger blogger) { return bloggerService.create(blogger); }

    @Operation(summary = "完整更新博主档案")
    @PutMapping("/{id}")
    public Blogger update(@PathVariable long id, @Valid @RequestBody Blogger blogger) {
        return bloggerService.update(id, blogger);
    }

    @Operation(summary = "删除博主档案")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { bloggerService.delete(id); }
}

