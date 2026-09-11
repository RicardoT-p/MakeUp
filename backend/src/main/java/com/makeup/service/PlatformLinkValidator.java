package com.makeup.service;

import com.makeup.model.Blogger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PlatformLinkValidator {

    private static final Pattern DOUYIN_VIDEO = Pattern.compile("https://(?:www\\.)?douyin\\.com/video/(\\d+)");
    private static final Pattern MISSING = Pattern.compile(
            "404页面不见了|作品已删除|视频不存在|内容不存在|页面不存在|笔记已删除|笔记不存在");
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private final StringRedisTemplate redisTemplate;
    private final BloggerService bloggerService;

    public PlatformLinkValidator(StringRedisTemplate redisTemplate, BloggerService bloggerService) {
        this.redisTemplate = redisTemplate;
        this.bloggerService = bloggerService;
    }

    public boolean isValid(String url) {
        String cached = readCache(url);
        return cached == null ? refresh(url) == Status.VALID : Status.VALID.name().equals(cached);
    }

    Status refresh(String url) {
        Status status = check(url);
        if (status != Status.UNKNOWN) writeCache(url, status);
        return status;
    }

    @Scheduled(fixedDelayString = "${makeup.link-validation.interval:PT15M}")
    public void recheckStoredLinks() {
        for (Blogger blogger : bloggerService.findAll()) {
            if (refresh(blogger.getPlatformUrl()) == Status.INVALID) {
                blogger.setEnabled(false);
                bloggerService.update(blogger.getId(), blogger);
            }
        }
    }

    private Status check(String url) {
        String validationUrl = validationUrl(url);
        if (validationUrl == null) return Status.INVALID;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(validationUrl)).timeout(Duration.ofSeconds(15))
                    .header("User-Agent", validationUrl.contains("iesdouyin.com") ? "Baiduspider" :
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36")
                    .header("Accept-Language", "zh-CN,zh;q=0.9").GET().build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 404 || response.statusCode() == 410 || MISSING.matcher(response.body()).find()) {
                return Status.INVALID;
            }
            return response.statusCode() >= 200 && response.statusCode() < 400 ? Status.VALID : Status.UNKNOWN;
        } catch (IOException exception) {
            return Status.UNKNOWN;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Status.UNKNOWN;
        }
    }

    static String validationUrl(String url) {
        Matcher douyin = DOUYIN_VIDEO.matcher(url);
        if (douyin.find()) return "https://www.iesdouyin.com/share/video/" + douyin.group(1) + "/";
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || host == null) return null;
            return host.equals("xiaohongshu.com") || host.endsWith(".xiaohongshu.com") ? url : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String cacheKey(String url) {
        return "makeup:link-status:" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(url.getBytes(StandardCharsets.UTF_8));
    }

    private String readCache(String url) {
        try {
            return redisTemplate.opsForValue().get(cacheKey(url));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void writeCache(String url, Status status) {
        try {
            redisTemplate.opsForValue().set(cacheKey(url), status.name(), CACHE_TTL);
        } catch (RuntimeException ignored) {
            // Redis 不可用时仍执行实时校验，避免缓存故障影响推荐。
        }
    }

    enum Status { VALID, INVALID, UNKNOWN }
}
