package io.github.wamukat.thymeleaflet.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelValueInferenceServiceTest {

    private final ModelValueInferenceService service = new ModelValueInferenceService();

    @Test
    void shouldInferBooleanNumberDateEmailAndString() {
        assertThat(service.inferLeafValue("hasNext")).isEqualTo(false);
        assertThat(service.inferLeafValue("totalPoints")).isEqualTo(0);
        assertThat(service.inferLeafValue("createdDate")).isEqualTo("2026-01-01");
        assertThat(service.inferLeafValue("email")).isEqualTo("sample@example.com");
        assertThat(service.inferLeafValue("displayName")).isEqualTo("Sample displayName");
    }
}
