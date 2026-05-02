package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoArgFragmentReferencePreProcessorTest {

    @Test
    void shouldNormalizeNoArgFragmentSelector() {
        String normalized = NoArgFragmentReferencePreProcessor.normalizeNoArgFragmentSelector(
            "~{components/topbar :: topbar()}"
        );

        assertEquals("~{components/topbar :: topbar}", normalized);
    }

    @Test
    void shouldNormalizeQuotedNoArgFragmentSelector() {
        String normalized = NoArgFragmentReferencePreProcessor.normalizeNoArgFragmentSelector(
            "~{'components/topbar' :: 'topbar()'}"
        );

        assertEquals("~{'components/topbar' :: 'topbar'}", normalized);
    }

    @Test
    void shouldNormalizeNoArgFragmentSelectorWithWhitespace() {
        String normalized = NoArgFragmentReferencePreProcessor.normalizeNoArgFragmentSelector(
            "~{components/topbar ::   topbar (   ) }"
        );

        assertEquals("~{components/topbar ::   topbar }", normalized);
    }

    @Test
    void shouldNormalizeNoArgFragmentSelectorWithOuterWhitespace() {
        String normalized = NoArgFragmentReferencePreProcessor.normalizeNoArgFragmentSelector(
            "  ~{components/topbar :: topbar()}  "
        );

        assertEquals("  ~{components/topbar :: topbar}  ", normalized);
    }

    @Test
    void shouldKeepPositionalParameterizedFragmentSelector() {
        String value = "~{components/button :: button(label)}";

        assertEquals(value, NoArgFragmentReferencePreProcessor.normalizeNoArgFragmentSelector(value));
    }

    @Test
    void shouldKeepNamedParameterizedFragmentSelector() {
        String value = "~{components/button :: button(label=${label})}";

        assertEquals(value, NoArgFragmentReferencePreProcessor.normalizeNoArgFragmentSelector(value));
    }

    @Test
    void shouldKeepNestedNoArgReferenceInsideParameterizedSelectorArgument() {
        String value = "~{components/shell :: shell(content=~{components/topbar :: topbar()})}";

        assertEquals(value, NoArgFragmentReferencePreProcessor.normalizeNoArgFragmentSelector(value));
    }

    @Test
    void shouldKeepNoArgTextInsideDynamicExpressionStringLiteral() {
        String value = "${view.fragmentExpression('components/topbar :: topbar()')}";

        assertEquals(value, NoArgFragmentReferencePreProcessor.normalizeNoArgFragmentSelector(value));
    }
}
