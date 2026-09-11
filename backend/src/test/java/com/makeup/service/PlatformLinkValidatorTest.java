package com.makeup.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformLinkValidatorTest {

    @Test
    void usesDouyinSharePageForContentLevelValidation() {
        assertThat(PlatformLinkValidator.validationUrl(
                "https://www.douyin.com/video/7588103890319494450?source=360Spider-sdc"))
                .isEqualTo("https://www.iesdouyin.com/share/video/7588103890319494450/");
        assertThat(PlatformLinkValidator.validationUrl("https://example.com/video/1")).isNull();
    }
}
