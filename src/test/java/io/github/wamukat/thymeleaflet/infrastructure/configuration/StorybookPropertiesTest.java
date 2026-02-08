package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorybookPropertiesTest {

    @Test
    void rawProperties_acceptNullAssignmentsWithoutValidation() {
        StorybookProperties properties = new StorybookProperties();
        StorybookProperties.ResourceConfig resources = new StorybookProperties.ResourceConfig();
        StorybookProperties.PreviewConfig preview = new StorybookProperties.PreviewConfig();

        properties.setBasePath(null);
        properties.setResources(null);
        properties.setCache(null);
        properties.setPreview(null);

        resources.setTemplatePaths(null);
        resources.setStylesheets(null);
        resources.setScripts(null);
        resources.setCacheDurationSeconds(0);

        preview.setBackgroundLight(null);
        preview.setBackgroundDark(null);
        preview.setViewports(null);

        assertThat(properties.getBasePath()).isNull();
        assertThat(properties.getResources()).isNull();
        assertThat(properties.getCache()).isNull();
        assertThat(properties.getPreview()).isNull();
        assertThat(resources.getTemplatePaths()).isNull();
        assertThat(resources.getStylesheets()).isNull();
        assertThat(resources.getScripts()).isNull();
        assertThat(resources.getCacheDurationSeconds()).isZero();
        assertThat(preview.getBackgroundLight()).isNull();
        assertThat(preview.getBackgroundDark()).isNull();
        assertThat(preview.getViewports()).isNull();
    }

    @Test
    void rawViewportPreset_allowsNullableFields() {
        StorybookProperties.ViewportPreset preset = new StorybookProperties.ViewportPreset();
        preset.setId(null);
        preset.setLabel(null);
        preset.setLabelKey(null);
        preset.setWidth(null);
        preset.setHeight(null);

        assertThat(preset.getId()).isNull();
        assertThat(preset.getLabel()).isNull();
        assertThat(preset.getLabelKey()).isNull();
        assertThat(preset.getWidth()).isNull();
        assertThat(preset.getHeight()).isNull();
    }
}
