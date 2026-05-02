package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JavaDocTagParserTest {

    private final JavaDocTagParser parser = new JavaDocTagParser();

    @Test
    void parse_shouldHandleGenericParamTypesDefaultMarkersAllowedValuesAndMultilineDescriptions() {
        JavaDocTagParser.ParsedTags parsed = parser.parse("""
            /**
             * Table component.
             * @param rows {@code Map<String, List<Item>>} [optional=empty] Row map.
             * Supports grouped entries.
             * values: "active", "archived"
             */
            """);

        assertThat(parsed.parameters()).singleElement()
            .satisfies(parameter -> {
                assertThat(parameter.getName()).isEqualTo("rows");
                assertThat(parameter.getType()).isEqualTo("Map<String, List<Item>>");
                assertThat(parameter.isRequired()).isFalse();
                assertThat(parameter.getDefaultValueOptional()).contains("empty");
                assertThat(parameter.getDescription()).isEqualTo("Row map. Supports grouped entries");
                assertThat(parameter.getAllowedValues()).containsExactly("active", "archived");
            });
    }

    @Test
    void parse_shouldHandleModelPathsGenericTypesAndMultilineDescriptions() {
        JavaDocTagParser.ParsedTags parsed = parser.parse("""
            /**
             * Activity list.
             * @model view.sections[].items {@code List<Map<String, Item>>} [required] Section items.
             * Rendered in display order.
             */
            """);

        assertThat(parsed.models()).singleElement()
            .satisfies(model -> {
                assertThat(model.getName()).isEqualTo("view.sections[].items");
                assertThat(model.getType()).isEqualTo("List<Map<String, Item>>");
                assertThat(model.isRequired()).isTrue();
                assertThat(model.getDescription()).isEqualTo("Section items. Rendered in display order.");
            });
    }

    @Test
    void parse_shouldSkipIncompleteParamAndModelTagsWithoutDroppingOtherTags() {
        JavaDocTagParser.ParsedTags parsed = parser.parse("""
            /**
             * Partial tags.
             * @param incomplete
             * @param label {@code String} [required] Label text
             * @model broken.path {@code String}
             * @model view.title {@code String} [optional=Untitled] Title text
             * @fragment partialTags
             */
            """);

        assertThat(parsed.parameters()).singleElement()
            .extracting(JavaDocAnalyzer.ParameterInfo::getName)
            .isEqualTo("label");
        assertThat(parsed.models()).singleElement()
            .satisfies(model -> {
                assertThat(model.getName()).isEqualTo("view.title");
                assertThat(model.getDefaultValueOptional()).contains("Untitled");
            });
        assertThat(parsed.fragmentName()).contains("partialTags");
    }
}
