package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResolvedStorybookConfigTest {

    @Test
    void from_appliesDefaultsWhenTopLevelSectionsAreNull() {
        StorybookProperties raw = new StorybookProperties();
        raw.setBasePath(null);
        raw.setResources(null);
        raw.setCache(null);
        raw.setPreview(null);

        ResolvedStorybookConfig resolved = ResolvedStorybookConfig.from(raw);

        assertThat(resolved.getBasePath()).isEqualTo("/thymeleaflet");
        assertThat(resolved.getResources().getTemplatePaths()).containsExactly("/templates/");
        assertThat(resolved.getPreview().getBackgroundLight()).isEqualTo("#f3f4f6");
        assertThat(resolved.getPreview().getBackgroundDark()).isEqualTo("#1f2937");
    }

    @Test
    void from_rejectsInvalidTemplatePathSettings() {
        StorybookProperties raw = new StorybookProperties();
        StorybookProperties.ResourceConfig resources = new StorybookProperties.ResourceConfig();
        resources.setTemplatePaths(List.of());
        raw.setResources(resources);

        assertThatThrownBy(() -> ResolvedStorybookConfig.from(raw))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one template path must be configured");
    }

    @Test
    void from_rejectsInvalidViewportDefinition() {
        StorybookProperties raw = new StorybookProperties();
        StorybookProperties.PreviewConfig preview = new StorybookProperties.PreviewConfig();
        StorybookProperties.ViewportPreset invalid = new StorybookProperties.ViewportPreset();
        invalid.setId("mobile");
        invalid.setWidth(0);
        invalid.setHeight(320);
        preview.setViewports(List.of(invalid));
        raw.setPreview(preview);

        assertThatThrownBy(() -> ResolvedStorybookConfig.from(raw))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Viewport width must be positive for id: mobile");
    }

    @Test
    void from_normalizesResourceLists() {
        StorybookProperties raw = new StorybookProperties();
        StorybookProperties.ResourceConfig resources = new StorybookProperties.ResourceConfig();
        resources.setTemplatePaths(List.of(" /templates/ ", "  "));
        resources.setStylesheets(List.of(" /css/a.css ", " "));
        resources.setScripts(List.of(" /js/a.js ", ""));
        raw.setResources(resources);

        ResolvedStorybookConfig resolved = ResolvedStorybookConfig.from(raw);

        assertThat(resolved.getResources().getTemplatePaths()).containsExactly("/templates/");
        assertThat(resolved.getResources().getStylesheets()).containsExactly("/css/a.css");
        assertThat(resolved.getResources().getScripts()).containsExactly("/js/a.js");
    }

    @Test
    void from_rejectsNonDefaultBasePath() {
        StorybookProperties raw = new StorybookProperties();
        raw.setBasePath("/thymeleaflet2");

        assertThatThrownBy(() -> ResolvedStorybookConfig.from(raw))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Only '/thymeleaflet' is currently supported for thymeleaflet.base-path. Configured value: /thymeleaflet2");
    }

    @Test
    void from_normalizesEquivalentBasePathToDefault() {
        StorybookProperties raw = new StorybookProperties();
        raw.setBasePath(" thymeleaflet/ ");

        ResolvedStorybookConfig resolved = ResolvedStorybookConfig.from(raw);

        assertThat(resolved.getBasePath()).isEqualTo("/thymeleaflet");
    }
}
