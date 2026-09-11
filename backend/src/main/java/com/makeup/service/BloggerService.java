package com.makeup.service;

import com.makeup.mapper.BloggerMapper;
import com.makeup.model.Blogger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class BloggerService {

    private final BloggerMapper bloggerMapper;

    public BloggerService(BloggerMapper bloggerMapper) {
        this.bloggerMapper = bloggerMapper;
    }

    @Cacheable("bloggers")
    public List<Blogger> findAll() {
        return bloggerMapper.findAllEnabled();
    }

    public Blogger find(long id) {
        return bloggerMapper.findById(id).orElseThrow(() -> new NoSuchElementException("未找到该博主"));
    }

    @CacheEvict(value = "bloggers", allEntries = true)
    public Blogger create(Blogger blogger) {
        blogger.setId(null);
        bloggerMapper.insert(blogger);
        return blogger;
    }

    public Blogger saveDiscovered(Blogger blogger) {
        return bloggerMapper.findByPlatformUrl(blogger.getPlatformUrl())
                .map(existing -> update(existing.getId(), blogger))
                .orElseGet(() -> create(blogger));
    }

    @CacheEvict(value = "bloggers", allEntries = true)
    public Blogger update(long id, Blogger blogger) {
        blogger.setId(id);
        if (bloggerMapper.update(blogger) == 0) {
            throw new NoSuchElementException("未找到该博主");
        }
        return blogger;
    }

    @CacheEvict(value = "bloggers", allEntries = true)
    public void delete(long id) {
        if (bloggerMapper.delete(id) == 0) {
            throw new NoSuchElementException("未找到该博主");
        }
    }
}
