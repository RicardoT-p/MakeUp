package com.makeup.api;

import com.makeup.model.FaceMetrics;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class AnalysisModels {

    private AnalysisModels() {
    }

    public record AnalysisRequest(
            @NotNull @Valid FaceMetrics metrics,
            @Min(0) @Max(100) int qualityScore) {
    }

    public record Recommendation(
            long id,
            int rank,
            String displayName,
            String platform,
            String platformUrl,
            String avatarUrl,
            List<String> styleTags,
            int score,
            String bestPart,
            List<String> reasons,
            String focus) {
    }

    public record AnalysisResponse(
            long analysisId,
            Instant createdAt,
            FaceMetrics metrics,
            List<Recommendation> recommendations) {
    }
}

