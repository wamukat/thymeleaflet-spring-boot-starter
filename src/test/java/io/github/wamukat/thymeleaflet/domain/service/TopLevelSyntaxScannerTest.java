package io.github.wamukat.thymeleaflet.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TopLevelSyntaxScannerTest {

    private final TopLevelSyntaxScanner scanner = new TopLevelSyntaxScanner();

    @Test
    void findFirst_shouldFindTokenOnlyAtTopLevel() {
        String value = "layout :: wrapper(label='a :: b', child=~{child :: body()}) :: target";

        assertThat(scanner.findFirst(value, "::")).hasValue(7);
    }

    @Test
    void findFirst_shouldSupportBoundedSearch() {
        String value = "prefix :: card()";
        int bodyStart = "prefix :: ".length();

        assertThat(scanner.findFirst(value, "(", bodyStart, value.length()))
            .hasValue("prefix :: card".length());
    }

    @Test
    void split_shouldIgnoreSeparatorsInsideQuotesAndNestedSyntax() {
        TopLevelSyntaxScanner.SplitResult result = scanner.split(
            "label='a,b', items=${view.items[0]}, child=~{x :: y(a='b,c')}",
            ','
        );

        assertThat(result.isBalanced()).isTrue();
        assertThat(result.segments()).containsExactly(
            "label='a,b'",
            " items=${view.items[0]}",
            " child=~{x :: y(a='b,c')}"
        );
    }

    @Test
    void split_shouldReportUnbalancedNestedSyntax() {
        TopLevelSyntaxScanner.SplitResult result = scanner.split("label=${view.title", ',');

        assertThat(result.isBalanced()).isFalse();
        assertThat(result.segments()).containsExactly("label=${view.title");
    }
}
