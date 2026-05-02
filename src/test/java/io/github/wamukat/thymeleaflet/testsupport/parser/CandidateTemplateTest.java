package io.github.wamukat.thymeleaflet.testsupport.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CandidateTemplateTest {

    @Test
    void subtree_shouldKeepSiblingBoundaries() {
        CandidateElement root = element("section", 0, -1, Map.of("th:fragment", "root"));
        CandidateElement child = element("p", 1, 0, Map.of("th:text", "${view.root}"));
        CandidateElement sibling = element("section", 2, -1, Map.of("th:fragment", "sibling"));
        CandidateElement siblingChild = element("p", 3, 2, Map.of("th:text", "${view.sibling}"));
        CandidateTemplate template = new CandidateTemplate(List.of(root, child, sibling, siblingChild));

        assertThat(template.subtree(root))
            .extracting(CandidateElement::index)
            .containsExactly(0, 1);
    }

    @Test
    void subtree_shouldStopAtBrokenParentChains() {
        CandidateElement root = element("section", 0, -1, Map.of("th:fragment", "root"));
        CandidateElement brokenDescendant = element("p", 1, 99, Map.of("th:text", "${view.broken}"));
        CandidateTemplate template = new CandidateTemplate(List.of(root, brokenDescendant));

        assertThat(template.subtree(root))
            .extracting(CandidateElement::index)
            .containsExactly(0);
    }

    @Test
    void constructor_shouldRejectDuplicateCandidateIndexes() {
        CandidateElement first = element("section", 0, -1, Map.of());
        CandidateElement duplicate = element("p", 0, -1, Map.of());

        assertThatThrownBy(() -> new CandidateTemplate(List.of(first, duplicate)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Candidate element indexes must be unique: 0");
    }

    @Test
    void hasSourcePositions_shouldReflectCandidateCapability() {
        CandidateElement root = element("section", 0, -1, Map.of());

        assertThat(new CandidateTemplate(List.of(root)).hasSourcePositions()).isFalse();
        assertThat(new CandidateTemplate(List.of(root), true).hasSourcePositions()).isTrue();
    }

    private static CandidateElement element(
        String name,
        int index,
        int parentIndex,
        Map<String, String> attributes
    ) {
        return new CandidateElement(name, attributes, index, parentIndex, parentIndex < 0 ? 0 : 1);
    }
}
