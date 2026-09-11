package com.makeup.service;

import com.makeup.model.FaceMetrics;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreatorDiscoveryServiceTest {

    @Test
    void keepsOnlySupportedHttpsPlatformResults() throws Exception {
        var html = """
                <a data-mdurl="https://www.xiaohongshu.com/explore/abc"><em>冷感</em>眼妆 - 小红书</a>
                <a data-mdurl="https://example.com/video">伪造结果</a>
                <a data-mdurl="http://www.douyin.com/video/1">不安全链接</a>
                """;
        var metrics = new FaceMetrics(1.4, .75, .3, .3, .24, 3.2, .24, .3, .13, .35);

        var results = CreatorDiscoveryService.parseResults(html,
                new CreatorDiscoveryService.SearchPlan("query", "眼妆,清冷", "眼|睫毛"), metrics);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDisplayName()).isEqualTo("冷感眼妆");
        assertThat(results.get(0).getPlatform()).isEqualTo("小红书");
    }

}
