package com.makeup.api;

import com.makeup.api.AnalysisModels.AnalysisRequest;
import com.makeup.api.AnalysisModels.AnalysisResponse;
import com.makeup.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "面部分析")
@RestController
@RequestMapping("/api/v1/analyses")
public class AnalysisController {

    private final RecommendationService recommendationService;

    public AnalysisController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Operation(summary = "保存合格的面部比例并生成按部位推荐")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse create(@Valid @RequestBody AnalysisRequest request) {
        return recommendationService.analyze(request.metrics(), request.qualityScore());
    }

    @Operation(summary = "读取历史分析")
    @GetMapping("/{id}")
    public AnalysisResponse find(@PathVariable long id) {
        return recommendationService.find(id);
    }
}

