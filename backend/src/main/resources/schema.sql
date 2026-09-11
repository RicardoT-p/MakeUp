CREATE TABLE IF NOT EXISTS beauty_blogger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    display_name VARCHAR(80) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    platform_url VARCHAR(500) NOT NULL,
    avatar_url VARCHAR(500),
    style_tags VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    face_length_width DECIMAL(7,4) NOT NULL,
    jaw_cheek_width DECIMAL(7,4) NOT NULL,
    upper_third_cheek_width DECIMAL(7,4) NOT NULL,
    lower_third_ratio DECIMAL(7,4) NOT NULL,
    eye_spacing_face_width DECIMAL(7,4) NOT NULL,
    eye_aspect_ratio DECIMAL(7,4) NOT NULL,
    nose_width_ratio DECIMAL(7,4) NOT NULL,
    eyebrow_width_ratio DECIMAL(7,4) NOT NULL,
    eyebrow_thickness_ratio DECIMAL(7,4) NOT NULL,
    mouth_width_ratio DECIMAL(7,4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS face_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metrics_json JSON NOT NULL,
    quality_score TINYINT UNSIGNED NOT NULL,
    result_json JSON NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_quality_score CHECK (quality_score <= 100)
);

