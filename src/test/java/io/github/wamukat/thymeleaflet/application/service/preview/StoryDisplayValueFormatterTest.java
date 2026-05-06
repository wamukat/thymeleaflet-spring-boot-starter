package io.github.wamukat.thymeleaflet.application.service.preview;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StoryDisplayValueFormatterTest {

    private final StoryDisplayValueFormatter formatter = new StoryDisplayValueFormatter(new ObjectMapper());

    @Test
    void format_prettyPrintsNestedModelValues() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("hasNext", true);
        view.put("items", List.of("A", "B"));
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("view", view);

        String formatted = formatter.format(model);

        assertThat(formatted).contains("{\n");
        assertThat(formatted).contains("  \"view\" : {");
        assertThat(formatted).contains("    \"hasNext\" : true");
        assertThat(formatted).contains("    \"items\" : [ \"A\", \"B\" ]");
    }

    @Test
    void format_keepsSimpleValuesCompact() {
        assertThat(formatter.format("primary")).isEqualTo("primary");
        assertThat(formatter.format(12)).isEqualTo("12");
        assertThat(formatter.format(true)).isEqualTo("true");
    }

    @Test
    void formatValues_preservesInputOrder() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("first", Map.of("value", 1));
        values.put("second", List.of("x"));

        Map<String, String> formattedValues = formatter.formatValues(values);

        assertThat(formattedValues.keySet()).containsExactly("first", "second");
        assertThat(formattedValues.get("first")).contains("\"value\" : 1");
        assertThat(formattedValues.get("second")).isEqualTo("[ \"x\" ]");
    }
}
