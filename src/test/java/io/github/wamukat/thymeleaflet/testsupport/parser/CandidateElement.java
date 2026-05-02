package io.github.wamukat.thymeleaflet.testsupport.parser;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record CandidateElement(
    String name,
    Map<String, String> attributes,
    int index,
    int parentIndex,
    int depth
) {
    public CandidateElement {
        name = name.trim();
        attributes = Map.copyOf(attributes);
    }

    public Optional<String> attributeValue(String attributeName) {
        Objects.requireNonNull(attributeName, "attributeName cannot be null");
        return Optional.ofNullable(attributes.get(attributeName));
    }

    public List<String> thymeleafAttributes() {
        return attributes.entrySet().stream()
            .filter(entry -> isThymeleafAttribute(entry.getKey()))
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .toList();
    }

    private static boolean isThymeleafAttribute(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.startsWith("th:") || normalized.startsWith("data-th-");
    }
}
