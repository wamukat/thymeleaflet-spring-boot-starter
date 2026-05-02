package io.github.wamukat.thymeleaflet.domain.service;

import java.util.Locale;
import java.util.Set;

public final class FragmentReferenceAttributes {

    private static final Set<String> REFERENCE_ATTRIBUTES = Set.of(
        "th:replace",
        "th:insert",
        "th:include",
        "data-th-replace",
        "data-th-insert",
        "data-th-include"
    );

    private static final Set<String> INSERTION_ATTRIBUTES = Set.of(
        "th:replace",
        "th:insert",
        "data-th-replace",
        "data-th-insert"
    );

    private static final Set<String> REPLACEMENT_ATTRIBUTES = Set.of(
        "th:replace",
        "data-th-replace"
    );

    private FragmentReferenceAttributes() {
    }

    public static boolean isReferenceAttribute(String attributeName) {
        return REFERENCE_ATTRIBUTES.contains(normalize(attributeName));
    }

    public static boolean isInsertionAttribute(String attributeName) {
        return INSERTION_ATTRIBUTES.contains(normalize(attributeName));
    }

    public static boolean isReplacementAttribute(String attributeName) {
        return REPLACEMENT_ATTRIBUTES.contains(normalize(attributeName));
    }

    private static String normalize(String attributeName) {
        return attributeName.toLowerCase(Locale.ROOT);
    }
}
