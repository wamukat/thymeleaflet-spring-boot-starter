package io.github.wamukat.thymeleaflet.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wamukat.thymeleaflet.domain.model.FragmentExpression;
import org.junit.jupiter.api.Test;

class FragmentExpressionParserTest {

    private final FragmentExpressionParser parser = new FragmentExpressionParser();

    @Test
    void parse_shouldReadTemplateFragmentAndArguments() {
        FragmentExpression expression = parser.parse("~{components/card :: card(title=${view.title}, variant='primary')}")
            .orElseThrow();

        assertThat(expression.templatePath()).isEqualTo("components/card");
        assertThat(expression.fragmentName()).isEqualTo("card");
        assertThat(expression.arguments()).containsExactly("title=${view.title}", "variant='primary'");
    }

    @Test
    void parse_shouldSupportQuotedTemplatePathAndNoArgFragment() {
        FragmentExpression expression = parser.parse("~{'components/topbar' :: 'topbar()'}")
            .orElseThrow();

        assertThat(expression.templatePath()).isEqualTo("components/topbar");
        assertThat(expression.fragmentName()).isEqualTo("topbar");
        assertThat(expression.arguments()).isEmpty();
    }

    @Test
    void parse_shouldSupportPositionalArgumentsWithNestedSyntax() {
        FragmentExpression expression = parser.parse(
                "~{components/list :: item(${view.items[0].label}, ${#temporals.format(view.date, 'yyyy, MM')})}"
            )
            .orElseThrow();

        assertThat(expression.templatePath()).isEqualTo("components/list");
        assertThat(expression.fragmentName()).isEqualTo("item");
        assertThat(expression.arguments())
            .containsExactly("${view.items[0].label}", "${#temporals.format(view.date, 'yyyy, MM')}");
    }

    @Test
    void parse_shouldFailClosedForMalformedOrDynamicInput() {
        assertThat(parser.parse("${dynamicRef}")).isEmpty();
        assertThat(parser.parse("~{components/card}")).isEmpty();
        assertThat(parser.parse("~{components/card :: card(label=${view.title)}")).isEmpty();
        assertThat(parser.parse("~{${dynamicPath} :: card()}")).isEmpty();
    }

    @Test
    void parseWithDiagnostics_shouldReturnNoWarningsForValidExpression() {
        FragmentExpressionParser.FragmentExpressionParseResult result =
            parser.parseWithDiagnostics("~{components/card :: card(title='Hello')}");

        assertThat(result.expression()).isPresent();
        assertThat(result.diagnostics()).isEmpty();
    }

    @Test
    void parseWithDiagnostics_shouldWarnForMalformedAndDynamicExpressions() {
        FragmentExpressionParser.FragmentExpressionParseResult malformed =
            parser.parseWithDiagnostics("~{components/card}");
        FragmentExpressionParser.FragmentExpressionParseResult dynamic =
            parser.parseWithDiagnostics("${dynamicRef}");

        assertThat(malformed.expression()).isEmpty();
        assertThat(malformed.diagnostics()).singleElement()
            .satisfies(diagnostic -> {
                assertThat(diagnostic.code()).isEqualTo("FRAGMENT_EXPRESSION_MALFORMED");
                assertThat(diagnostic.message()).contains("components/card");
            });
        assertThat(dynamic.expression()).isEmpty();
        assertThat(dynamic.diagnostics()).singleElement()
            .satisfies(diagnostic -> {
                assertThat(diagnostic.code()).isEqualTo("FRAGMENT_EXPRESSION_DYNAMIC");
                assertThat(diagnostic.message()).contains("dynamicRef");
            });
    }
}
