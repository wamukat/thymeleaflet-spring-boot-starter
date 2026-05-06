package io.github.wamukat.thymeleaflet.application.service.preview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StoryDisplayValueFormatter {

    private final ObjectMapper objectMapper;

    public StoryDisplayValueFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String format(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        if (isSimpleValue(value)) {
            return String.valueOf(value);
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return String.valueOf(value);
        }
    }

    public Map<String, String> formatValues(Map<String, Object> values) {
        Map<String, String> formattedValues = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            formattedValues.put(entry.getKey(), format(entry.getValue()));
        }
        return formattedValues;
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof CharSequence
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof Character
            || value instanceof Enum<?>;
    }
}
