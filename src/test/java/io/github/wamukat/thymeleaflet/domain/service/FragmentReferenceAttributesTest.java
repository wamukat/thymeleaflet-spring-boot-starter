package io.github.wamukat.thymeleaflet.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FragmentReferenceAttributesTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "th:replace",
        "th:insert",
        "th:include",
        "data-th-replace",
        "data-th-insert",
        "data-th-include"
    })
    void isReferenceAttribute_shouldCoverStaticFragmentReferenceAttributes(String attributeName) {
        assertThat(FragmentReferenceAttributes.isReferenceAttribute(attributeName)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"th:replace", "th:insert", "data-th-replace", "data-th-insert"})
    void isInsertionAttribute_shouldCoverReplaceAndInsertOnly(String attributeName) {
        assertThat(FragmentReferenceAttributes.isInsertionAttribute(attributeName)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"th:include", "data-th-include", "th:text"})
    void isInsertionAttribute_shouldExcludeIncludeAndNonReferenceAttributes(String attributeName) {
        assertThat(FragmentReferenceAttributes.isInsertionAttribute(attributeName)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"th:replace", "data-th-replace"})
    void isReplacementAttribute_shouldCoverJavaDocExampleReplacementAttributes(String attributeName) {
        assertThat(FragmentReferenceAttributes.isReplacementAttribute(attributeName)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"th:insert", "data-th-insert", "th:include", "data-th-include", "th:text"})
    void isReplacementAttribute_shouldExcludeNonReplacementAttributes(String attributeName) {
        assertThat(FragmentReferenceAttributes.isReplacementAttribute(attributeName)).isFalse();
    }
}
