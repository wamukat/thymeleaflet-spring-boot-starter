package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FragmentModelInferenceServiceTest {

    private final FragmentModelInferenceService service =
        new FragmentModelInferenceService(new DefaultResourceLoader());

    @Test
    void shouldInferModelFromTemplateExpressions() {
        Map<String, Object> inferred = service.inferModel(
            "fragments/model-inference-sample",
            "modelInferenceSample",
            List.of("label")
        );

        assertThat(inferred).containsKeys("displayName", "isActive", "member");
        assertThat(inferred.get("displayName")).isEqualTo("Sample displayName");
        assertThat(inferred.get("isActive")).isEqualTo(false);
        assertThat(inferred).doesNotContainKey("label");

        assertThat(inferred).containsEntry("member", Map.of("status", "Sample status"));
    }
}
