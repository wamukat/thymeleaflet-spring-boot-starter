package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JavaDocFallbackValueServiceTest {

    private final JavaDocFallbackValueService service = new JavaDocFallbackValueService();

    @Test
    void shouldPreferParamDefaultsAndStaticExampleArguments() {
        JavaDocAnalyzer.JavaDocInfo docInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "Button",
            List.of(JavaDocAnalyzer.ParameterInfo.optional("label", "String", "Fallback label", "label")),
            List.of(),
            List.of(JavaDocAnalyzer.ExampleInfo.of(
                "components/button",
                "primaryButton",
                List.of("label='Example label'", "variant='primary'", "unsafe=${dynamic}")
            ), JavaDocAnalyzer.ExampleInfo.of(
                "",
                "primaryButton",
                List.of("size='lg'")
            )),
            Optional.empty()
        );

        assertThat(service.parameterDefaults(docInfo, "components/button", "primaryButton"))
            .containsEntry("label", "Fallback label")
            .containsEntry("variant", "primary")
            .containsEntry("size", "lg")
            .doesNotContainKey("unsafe");
    }

    @Test
    void shouldExtractModelDefaultsAsNestedPaths() {
        JavaDocAnalyzer.JavaDocInfo docInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "Form",
            List.of(),
            List.of(
                JavaDocAnalyzer.ModelInfo.optional("profile.email", "String", "user@example.com", "email"),
                JavaDocAnalyzer.ModelInfo.optional("profile.enabled", "Boolean", "true", "enabled")
            ),
            List.of(),
            Optional.empty()
        );

        assertThat(service.modelDefaults(docInfo)).containsEntry(
            "profile",
            java.util.Map.of("email", "user@example.com", "enabled", true)
        );
    }
}
