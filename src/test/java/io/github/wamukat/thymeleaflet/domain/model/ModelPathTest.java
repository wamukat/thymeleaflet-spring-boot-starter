package io.github.wamukat.thymeleaflet.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelPathTest {

    @Test
    void shouldInferSampleValuesByLeafName() {
        assertThat(ModelPath.of(List.of("root", "hasNext")).inferSampleValue()).isEqualTo(false);
        assertThat(ModelPath.of(List.of("root", "totalPoints")).inferSampleValue()).isEqualTo(0);
        assertThat(ModelPath.of(List.of("root", "createdDate")).inferSampleValue()).isEqualTo("2026-01-01");
        assertThat(ModelPath.of(List.of("root", "email")).inferSampleValue()).isEqualTo("sample@example.com");
        assertThat(ModelPath.of(List.of("root", "errorMessage")).inferSampleValue()).isEqualTo("Sample errorMessage");
        assertThat(ModelPath.of(List.of("root", "successMessage")).inferSampleValue()).isEqualTo("Sample successMessage");
        assertThat(ModelPath.of(List.of("root", "displayName")).inferSampleValue()).isEqualTo("Sample displayName");
    }

    @Test
    void shouldExposeRootAndSubPath() {
        ModelPath path = ModelPath.of(List.of("pointPage", "items", "amount"));
        assertThat(path.root()).isEqualTo("pointPage");
        assertThat(path.subPathWithoutRoot()).containsExactly("items", "amount");
    }
}
