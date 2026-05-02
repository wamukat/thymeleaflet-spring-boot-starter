package io.github.wamukat.thymeleaflet.testsupport.parser;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record CandidateTemplate(List<CandidateElement> elements) {
    public CandidateTemplate {
        elements = List.copyOf(elements);
    }

    public List<String> thymeleafAttributes() {
        return elements.stream()
            .flatMap(element -> element.thymeleafAttributes().stream())
            .toList();
    }

    public List<String> fragmentDefinitions() {
        return elements.stream()
            .flatMap(element -> element.attributeValue("th:fragment").stream())
            .toList();
    }

    public Optional<CandidateElement> element(String fragmentName) {
        return elements.stream()
            .filter(element -> element.attributeValue("th:fragment").filter(fragmentName::equals).isPresent())
            .findFirst();
    }

    public List<CandidateElement> subtree(CandidateElement root) {
        Map<Integer, CandidateElement> byIndex = elements.stream()
            .collect(Collectors.toUnmodifiableMap(CandidateElement::index, element -> element));
        Predicate<CandidateElement> isRootOrDescendant =
            element -> element.index() == root.index() || isDescendantOf(element, root.index(), byIndex);
        return elements.stream()
            .filter(isRootOrDescendant)
            .toList();
    }

    public boolean hasSourcePositions() {
        return false;
    }

    private static boolean isDescendantOf(
        CandidateElement candidate,
        int rootIndex,
        Map<Integer, CandidateElement> byIndex
    ) {
        int parentIndex = candidate.parentIndex();
        while (parentIndex >= 0) {
            if (parentIndex == rootIndex) {
                return true;
            }
            CandidateElement parent = byIndex.get(parentIndex);
            if (parent == null) {
                return false;
            }
            parentIndex = parent.parentIndex();
        }
        return false;
    }
}
