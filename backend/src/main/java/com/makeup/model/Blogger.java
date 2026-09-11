package com.makeup.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public class Blogger implements Serializable {

    private Long id;
    @NotBlank @Size(max = 80)
    private String displayName;
    @NotBlank @Size(max = 32)
    private String platform;
    @NotBlank @Pattern(regexp = "https://.*")
    private String platformUrl;
    @Pattern(regexp = "(/.*|https://.*)")
    private String avatarUrl;
    @NotBlank
    private String styleTags;
    private double faceLengthWidth;
    private double jawCheekWidth;
    private double upperThirdCheekWidth;
    private double lowerThirdRatio;
    private double eyeSpacingFaceWidth;
    private double eyeAspectRatio;
    private double noseWidthRatio;
    private double eyebrowWidthRatio;
    private double eyebrowThicknessRatio;
    private double mouthWidthRatio;
    private boolean enabled = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getPlatformUrl() { return platformUrl; }
    public void setPlatformUrl(String platformUrl) { this.platformUrl = platformUrl; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getStyleTags() { return styleTags; }
    public void setStyleTags(String styleTags) { this.styleTags = styleTags; }
    @Valid @NotNull
    public FaceMetrics getMetrics() { return new FaceMetrics(faceLengthWidth, jawCheekWidth, upperThirdCheekWidth,
            lowerThirdRatio, eyeSpacingFaceWidth, eyeAspectRatio, noseWidthRatio, eyebrowWidthRatio,
            eyebrowThicknessRatio, mouthWidthRatio); }
    public void setMetrics(FaceMetrics metrics) {
        this.faceLengthWidth = metrics.faceLengthWidth();
        this.jawCheekWidth = metrics.jawCheekWidth();
        this.upperThirdCheekWidth = metrics.upperThirdCheekWidth();
        this.lowerThirdRatio = metrics.lowerThirdRatio();
        this.eyeSpacingFaceWidth = metrics.eyeSpacingFaceWidth();
        this.eyeAspectRatio = metrics.eyeAspectRatio();
        this.noseWidthRatio = metrics.noseWidthRatio();
        this.eyebrowWidthRatio = metrics.eyebrowWidthRatio();
        this.eyebrowThicknessRatio = metrics.eyebrowThicknessRatio();
        this.mouthWidthRatio = metrics.mouthWidthRatio();
    }
    public void setFaceLengthWidth(double value) { this.faceLengthWidth = value; }
    public void setJawCheekWidth(double value) { this.jawCheekWidth = value; }
    public void setUpperThirdCheekWidth(double value) { this.upperThirdCheekWidth = value; }
    public void setLowerThirdRatio(double value) { this.lowerThirdRatio = value; }
    public void setEyeSpacingFaceWidth(double value) { this.eyeSpacingFaceWidth = value; }
    public void setEyeAspectRatio(double value) { this.eyeAspectRatio = value; }
    public void setNoseWidthRatio(double value) { this.noseWidthRatio = value; }
    public void setEyebrowWidthRatio(double value) { this.eyebrowWidthRatio = value; }
    public void setEyebrowThicknessRatio(double value) { this.eyebrowThicknessRatio = value; }
    public void setMouthWidthRatio(double value) { this.mouthWidthRatio = value; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
