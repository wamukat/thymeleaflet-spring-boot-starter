package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UnsafeFragmentInsertionDetectorTest {

    private final UnsafeFragmentInsertionDetector detector = new UnsafeFragmentInsertionDetector();

    @Test
    void findUnsafeParameter_shouldDetectMultilineThReplaceExpression() {
        String html = """
            <section th:fragment="shell(body)">
              <th:block
                  th:replace="${
                    body
                  }">
              </th:block>
            </section>
            """;

        assertThat(detector.findUnsafeParameter(html, Map.of("body", "components/card :: card")))
            .hasValue("body");
    }

    @ParameterizedTest
    @ValueSource(strings = {"th:replace", "th:insert", "data-th-replace", "data-th-insert"})
    void findUnsafeParameter_shouldDetectInsertionAttributes(String attributeName) {
        String html = """
            <section th:fragment="shell(content)">
              <div %s='${ content }'></div>
            </section>
            """.formatted(attributeName);

        assertThat(detector.findUnsafeParameter(html, Map.of("content", "components/card :: card")))
            .hasValue("content");
    }

    @Test
    void findUnsafeParameter_shouldIgnoreSafeLiteralFragmentExpressionValues() {
        String html = """
            <section th:fragment="shell(body)">
              <th:block th:replace="${body}"></th:block>
            </section>
            """;

        assertThat(detector.findUnsafeParameter(html, Map.of("body", "~{components/card :: card()}")))
            .isEmpty();
    }

    @Test
    void findUnsafeParameter_shouldIgnoreNonMatchingAndNonStringParameters() {
        String html = """
            <section th:fragment="shell(body)">
              <th:block th:replace="${body}"></th:block>
            </section>
            """;

        assertThat(detector.findUnsafeParameter(html, Map.of("body", 1, "other", "plain text")))
            .isEmpty();
    }
}
