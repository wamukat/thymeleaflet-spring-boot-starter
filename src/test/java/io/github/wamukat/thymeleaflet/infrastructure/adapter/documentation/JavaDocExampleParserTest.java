package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;
import org.junit.jupiter.api.Test;

class JavaDocExampleParserTest {

    private final JavaDocExampleParser parser = new JavaDocExampleParser(new StructuredTemplateParser());

    @Test
    void parseWithDiagnostics_shouldReturnNoWarningsForValidExamples() {
        JavaDocExampleParser.ExampleParseResult result = parser.parseWithDiagnostics("""
            /**
             * Valid example.
             * @example <div th:replace="~{components/card :: card(title='Hello')}"></div>
             */
            """);

        assertThat(result.examples()).singleElement()
            .satisfies(example -> {
                assertThat(example.getTemplatePath()).isEqualTo("components/card");
                assertThat(example.getFragmentName()).isEqualTo("card");
                assertThat(example.getArguments()).containsExactly("title='Hello'");
            });
        assertThat(result.diagnostics()).isEmpty();
    }

    @Test
    void parseWithDiagnostics_shouldResolveCurrentTemplateExamples() {
        JavaDocExampleParser.ExampleParseResult result = parser.parseWithDiagnostics("""
            /**
             * Current template example.
             * @example <div th:replace="~{:: card(title='Local')}"></div>
             */
            """, "components/card");

        assertThat(result.examples()).singleElement()
            .satisfies(example -> {
                assertThat(example.getTemplatePath()).isEqualTo("components/card");
                assertThat(example.getFragmentName()).isEqualTo("card");
                assertThat(example.getArguments()).containsExactly("title='Local'");
            });
        assertThat(result.diagnostics()).isEmpty();
    }

    @Test
    void parseWithDiagnostics_shouldWarnForInvalidExampleReferences() {
        JavaDocExampleParser.ExampleParseResult result = parser.parseWithDiagnostics("""
            /**
             * Invalid example.
             * @example <div th:replace="~{components/card}"></div>
             * @example <div data-th-replace="${dynamicReference}"></div>
             */
            """);

        assertThat(result.examples()).isEmpty();
        assertThat(result.diagnostics())
            .extracting(diagnostic -> diagnostic.code())
            .containsExactly(
                "FRAGMENT_EXPRESSION_MALFORMED",
                "TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED",
                "FRAGMENT_EXPRESSION_DYNAMIC"
            );
    }
}
