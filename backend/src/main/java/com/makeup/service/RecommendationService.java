package com.makeup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.makeup.api.AnalysisModels.AnalysisResponse;
import com.makeup.api.AnalysisModels.Recommendation;
import com.makeup.mapper.AnalysisMapper;
import com.makeup.model.AnalysisRecord;
import com.makeup.model.Blogger;
import com.makeup.model.FaceMetrics;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class RecommendationService {

    private final BloggerService bloggerService;
    private final CreatorDiscoveryService creatorDiscoveryService;
    private final AnalysisMapper analysisMapper;
    private final ObjectMapper objectMapper;

    public RecommendationService(BloggerService bloggerService, CreatorDiscoveryService creatorDiscoveryService,
                                 AnalysisMapper analysisMapper, ObjectMapper objectMapper) {
        this.bloggerService = bloggerService;
        this.creatorDiscoveryService = creatorDiscoveryService;
        this.analysisMapper = analysisMapper;
        this.objectMapper = objectMapper;
    }

    public AnalysisResponse analyze(FaceMetrics metrics, int qualityScore) {
        if (qualityScore < 60) {
            throw new IllegalArgumentException("照片质量不足，不参与推荐");
        }
        List<Recommendation> recommendations = recommend(metrics, creatorDiscoveryService.discover(metrics));
        AnalysisRecord record = new AnalysisRecord();
        record.setMetricsJson(json(metrics));
        record.setQualityScore(qualityScore);
        record.setResultJson(json(recommendations));
        analysisMapper.insert(record);
        return new AnalysisResponse(record.getId(), Instant.now(), metrics, recommendations);
    }

    public AnalysisResponse find(long id) {
        AnalysisRecord record = analysisMapper.findById(id)
                .orElseThrow(() -> new NoSuchElementException("未找到该分析记录"));
        try {
            FaceMetrics metrics = objectMapper.readValue(record.getMetricsJson(), FaceMetrics.class);
            var type = objectMapper.getTypeFactory().constructCollectionType(List.class, Recommendation.class);
            List<Recommendation> recommendations = objectMapper.readValue(record.getResultJson(), type);
            return new AnalysisResponse(record.getId(), record.getCreatedAt(), metrics, recommendations);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("分析记录无法读取", exception);
        }
    }

    List<Recommendation> recommend(FaceMetrics user) {
        return recommend(user, bloggerService.findAll());
    }

    List<Recommendation> recommend(FaceMetrics user, List<Blogger> bloggers) {
        if (bloggers.isEmpty()) {
            return List.of();
        }
        List<Recommendation> result = new ArrayList<>();
        for (int index = 0; index < Math.min(3, bloggers.size()); index++) {
            Blogger blogger = bloggers.get(index);
            String part = blogger.getStyleTags().contains("眼妆") ? "眼妆"
                    : blogger.getStyleTags().contains("唇妆") ? "唇妆" : "鼻部";
            result.add(toRecommendation(blogger, user, index + 1, part));
        }
        return result;
    }

    private Recommendation toRecommendation(Blogger blogger, FaceMetrics user, int rank, String part) {
        List<String> reasons = switch (part) {
            case "眼妆" -> List.of("已按你的双眼长宽比与眼间距生成眼妆检索词", "重点筛选眼线与眼影教程");
            case "唇妆" -> List.of("已按你的嘴宽占比生成唇妆检索词", "重点筛选唇峰与口红教程");
            case "鼻部" -> List.of("已按你的鼻翼宽度比例检索鼻影内容", "同时结合下颌与颧骨比例筛选修容主题");
            default -> List.of("已按你的脸长宽比与下颌比例检索", "重点筛选下颌修容和腮红内容");
        };
        String focus = switch (part) {
            case "眼妆" -> "眼线长度、眼影重心与睫毛方向";
            case "唇妆" -> "唇线、唇峰与口红晕染范围";
            case "鼻部" -> "鼻影宽度、鼻头提亮与鼻翼修饰";
            default -> "下颌修容、颧骨转折与腮红位置";
        };
        return new Recommendation(blogger.getId(), rank, blogger.getDisplayName(), blogger.getPlatform(),
                blogger.getPlatformUrl(), blogger.getAvatarUrl(), splitTags(blogger.getStyleTags()),
                Math.max(80, 96 - rank * 4), part, reasons, focus);
    }


    private List<String> splitTags(String tags) {
        return List.of(tags.split(","));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("数据序列化失败", exception);
        }
    }
}
