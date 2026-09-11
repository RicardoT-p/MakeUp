package com.makeup.service;

import com.makeup.model.Blogger;
import com.makeup.model.FaceMetrics;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CreatorDiscoveryService {

    private static final String SEARCH_URL = "https://www.so.com/s?q=";
    private static final String IMAGE_SEARCH_URL = "https://image.so.com/i?q=";
    private static final Pattern RESULT = Pattern.compile(
            "<a\\b[^>]*\\bdata-mdurl=\\\"([^\\\"]+)\\\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern THUMBNAIL = Pattern.compile(
            "\\\"thumb\\\":\\\"(https:\\\\/\\\\/[^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36";
    private final BloggerService bloggerService;
    private final PlatformLinkValidator linkValidator;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    public CreatorDiscoveryService(BloggerService bloggerService, PlatformLinkValidator linkValidator) {
        this.bloggerService = bloggerService;
        this.linkValidator = linkValidator;
    }

    public List<Blogger> discover(FaceMetrics metrics) {
        String faceTag = metrics.lowerThirdRatio() < .3 ? "短中庭" : "自然中庭";
        String styleTag = metrics.jawCheekWidth() < .75 ? "清冷" : "柔和轮廓";
        List<SearchPlan> plans = List.of(
                new SearchPlan("小红书 美妆 鼻影 下颌修容 腮红教程", faceTag + "," + styleTag + ",修容", "鼻|修容|腮红|下颌"),
                new SearchPlan("抖音 美妆 唇妆 口红教程", "唇妆,口红,教程", "唇|口红"),
                new SearchPlan("小红书 美妆 眼妆 眼线 眼影教程", "眼妆,眼线,眼影", "眼|睫毛"));
        List<Blogger> found = new ArrayList<>();
        Set<String> urls = new HashSet<>();
        for (SearchPlan plan : plans) {
            search(plan, metrics).stream().filter(plan::matches)
                    .filter(item -> urls.add(item.getPlatformUrl()))
                    .filter(this::completeAndValidate).findFirst()
                    .map(bloggerService::saveDiscovered).ifPresent(found::add);
        }
        if (found.isEmpty()) {
            throw new IllegalStateException("免费搜索未返回可用内容，可能触发了搜索引擎验证，请稍后重试");
        }
        return found;
    }

    private List<Blogger> search(SearchPlan plan, FaceMetrics metrics) {
        String query = URLEncoder.encode(plan.query(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(SEARCH_URL + query))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "zh-CN,zh;q=0.9").GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("免费搜索服务返回 HTTP " + response.statusCode());
            }
            return parseResults(response.body(), plan, metrics);
        } catch (IOException exception) {
            throw new IllegalStateException("免费搜索响应无法读取", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("免费搜索已中断", exception);
        }
    }

    private boolean completeAndValidate(Blogger blogger) {
        if (!linkValidator.isValid(blogger.getPlatformUrl())) return false;
        blogger.setAvatarUrl(findLiveCover(blogger.getDisplayName() + " " + blogger.getPlatform()));
        return blogger.getAvatarUrl() != null;
    }

    private String findLiveCover(String keywords) {
        String query = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
        try {
            HttpResponse<String> response = httpClient.send(request(IMAGE_SEARCH_URL + query),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) return null;
            var matcher = THUMBNAIL.matcher(response.body());
            while (matcher.find()) {
                String url = matcher.group(1).replace("\\/", "/");
                HttpResponse<Void> image = httpClient.send(request(url), HttpResponse.BodyHandlers.discarding());
                String contentType = image.headers().firstValue("Content-Type").orElse("");
                if (image.statusCode() >= 200 && image.statusCode() < 400 && contentType.startsWith("image/")) {
                    return url;
                }
            }
            return null;
        } catch (IOException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12))
                .header("User-Agent", USER_AGENT).header("Accept-Language", "zh-CN,zh;q=0.9").GET().build();
    }

    static List<Blogger> parseResults(String html, SearchPlan plan, FaceMetrics metrics) {
        List<Blogger> results = new ArrayList<>();
        var matcher = RESULT.matcher(html);
        while (matcher.find()) {
            String url = HtmlUtils.htmlUnescape(matcher.group(1));
            String title = clean(matcher.group(2));
            if (!isSupportedUrl(url) || title.isEmpty()) continue;
            Blogger blogger = new Blogger();
            blogger.setDisplayName(title.substring(0, Math.min(80, title.length())));
            blogger.setPlatform(url.contains("douyin.com") ? "抖音" : "小红书");
            blogger.setPlatformUrl(url);
            blogger.setAvatarUrl(null);
            blogger.setStyleTags(plan.tags());
            blogger.setMetrics(metrics);
            blogger.setEnabled(true);
            results.add(blogger);
        }
        return results;
    }

    private static boolean isSupportedUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            return "https".equals(uri.getScheme()) && host != null
                    && (host.equals("douyin.com") || host.endsWith(".douyin.com")
                    || host.equals("xiaohongshu.com") || host.endsWith(".xiaohongshu.com"));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String clean(String value) {
        return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]+>", ""))
                .replaceAll("\\s*[-—|]_?\\s*(抖音|小红书).*$", "").trim();
    }

    record SearchPlan(String query, String tags, String keywords) {
        boolean matches(Blogger blogger) {
            return Pattern.compile(keywords).matcher(blogger.getDisplayName()).find();
        }
    }
}
