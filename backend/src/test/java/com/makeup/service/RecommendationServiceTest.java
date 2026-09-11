package com.makeup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.makeup.model.Blogger;
import com.makeup.model.FaceMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceTest {

    @Test
    void returnsDistinctOverallMouthAndEyeReferences() {
        FaceMetrics user = metrics(1.45, .75, 3.2, .24, .36);
        List<Blogger> bloggers = List.of(
                blogger(1, metrics(1.45, .75, 3.2, .24, .33)),
                blogger(2, metrics(1.7, .6, 2.4, .31, .36)),
                blogger(3, metrics(1.6, .7, 3.2, .29, .31)));
        RecommendationService service = new RecommendationService(null, null, null, new ObjectMapper());

        var recommendations = service.recommend(user, bloggers);

        assertThat(recommendations).hasSize(3);
        assertThat(recommendations).extracting(item -> item.bestPart())
                .containsExactly("鼻部", "唇妆", "眼妆");
        assertThat(recommendations).extracting(item -> item.id()).doesNotHaveDuplicates();
    }

    private Blogger blogger(long id, FaceMetrics metrics) {
        Blogger blogger = new Blogger();
        blogger.setId(id);
        blogger.setDisplayName("示例" + id);
        blogger.setPlatform("小红书");
        blogger.setPlatformUrl("https://example.com/" + id);
        blogger.setAvatarUrl("/creator-1.png");
        blogger.setStyleTags(id == 2 ? "唇妆,通勤" : id == 3 ? "眼妆,清冷" : "修容,清冷");
        blogger.setMetrics(metrics);
        return blogger;
    }

    private FaceMetrics metrics(double face, double jaw, double eyeAspect, double nose, double mouth) {
        return new FaceMetrics(face, jaw, .3, .3, .24, eyeAspect, nose, .3, .13, mouth);
    }
}
