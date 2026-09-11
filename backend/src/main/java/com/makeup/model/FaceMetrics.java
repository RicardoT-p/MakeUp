package com.makeup.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.io.Serializable;

public record FaceMetrics(
        @DecimalMin("0.5") @DecimalMax("3.0") double faceLengthWidth,
        @DecimalMin("0.2") @DecimalMax("1.2") double jawCheekWidth,
        @DecimalMin("0.05") @DecimalMax("1.2") double upperThirdCheekWidth,
        @DecimalMin("0.05") @DecimalMax("0.8") double lowerThirdRatio,
        @DecimalMin("0.05") @DecimalMax("0.8") double eyeSpacingFaceWidth,
        @DecimalMin("1.0") @DecimalMax("8.0") double eyeAspectRatio,
        @DecimalMin("0.05") @DecimalMax("0.6") double noseWidthRatio,
        @DecimalMin("0.05") @DecimalMax("0.8") double eyebrowWidthRatio,
        @DecimalMin("0.01") @DecimalMax("0.5") double eyebrowThicknessRatio,
        @DecimalMin("0.05") @DecimalMax("0.8") double mouthWidthRatio) implements Serializable {
}
