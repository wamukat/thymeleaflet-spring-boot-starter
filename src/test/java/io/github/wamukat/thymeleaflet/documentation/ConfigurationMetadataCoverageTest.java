package io.github.wamukat.thymeleaflet.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.io.InputStream;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ConfigurationMetadataCoverageTest {

    @Test
    void springConfigurationMetadata_shouldDescribePrimaryThymeleafletProperties() throws Exception {
        JsonNode metadata = readMetadata();
        Map<String, JsonNode> properties = propertiesByName(metadata);

        assertThat(properties.keySet()).contains(
            "thymeleaflet.enabled",
            "thymeleaflet.base-path",
            "thymeleaflet.debug"
        );
        assertMetadata(
            properties,
            "thymeleaflet.resources.template-paths",
            "java.util.List<java.lang.String>",
            "trusted application template locations"
        );
        assertMetadata(
            properties,
            "thymeleaflet.resources.stylesheets",
            "java.util.List<java.lang.String>",
            "Stylesheet URLs injected into preview iframes"
        );
        assertMetadata(
            properties,
            "thymeleaflet.resources.scripts",
            "java.util.List<java.lang.String>",
            "Script URLs injected into preview iframes"
        );
        assertMetadata(
            properties,
            "thymeleaflet.resources.cache-duration-seconds",
            "java.lang.Integer",
            "Cache duration in seconds for static Thymeleaflet resources",
            "3600"
        );
        assertMetadata(
            properties,
            "thymeleaflet.cache.enabled",
            "java.lang.Boolean",
            "Enable Thymeleaflet in-memory caches",
            "true"
        );
        assertMetadata(
            properties,
            "thymeleaflet.cache.preload",
            "java.lang.Boolean",
            "Warm Thymeleaflet caches at startup",
            "false"
        );
        assertThat(properties.keySet()).contains(
            "thymeleaflet.preview.background-light",
            "thymeleaflet.preview.background-dark",
            "thymeleaflet.preview.viewports",
            "thymeleaflet.preview.viewports[].id",
            "thymeleaflet.preview.viewports[].label",
            "thymeleaflet.preview.viewports[].label-key",
            "thymeleaflet.preview.viewports[].width",
            "thymeleaflet.preview.viewports[].height",
            "thymeleaflet.security.auto-permit"
        );
    }

    private static JsonNode readMetadata() throws Exception {
        try (InputStream inputStream = ConfigurationMetadataCoverageTest.class.getResourceAsStream(
            "/META-INF/spring-configuration-metadata.json"
        )) {
            assertThat(inputStream).isNotNull();
            return new ObjectMapper().readTree(inputStream);
        }
    }

    private static Map<String, JsonNode> propertiesByName(JsonNode metadata) {
        Map<String, JsonNode> properties = new LinkedHashMap<>();
        metadata.path("properties").forEach(property -> properties.put(property.path("name").asText(), property));
        return properties;
    }

    private static void assertMetadata(
        Map<String, JsonNode> properties,
        String name,
        String type,
        String description
    ) {
        JsonNode property = requireProperty(properties, name);

        assertThat(property.path("type").asText()).isEqualTo(type);
        assertThat(property.path("description").asText()).contains(description);
    }

    private static void assertMetadata(
        Map<String, JsonNode> properties,
        String name,
        String type,
        String description,
        String defaultValue
    ) {
        JsonNode property = requireProperty(properties, name);

        assertThat(property.path("type").asText()).isEqualTo(type);
        assertThat(property.path("description").asText()).contains(description);
        assertThat(property.path("defaultValue").asText()).isEqualTo(defaultValue);
    }

    private static JsonNode requireProperty(Map<String, JsonNode> properties, String name) {
        if (!properties.containsKey(name)) {
            throw new AssertionError("Missing configuration metadata property: " + name);
        }
        return properties.get(name);
    }
}
