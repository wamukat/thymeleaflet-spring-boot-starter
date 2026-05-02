package io.github.wamukat.thymeleaflet.testsupport.parser;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record CandidateTemplate(List<CandidateElement> elements, boolean hasSourcePositions) {

    public CandidateTemplate(List<CandidateElement> elements) {
        this(elements, false);
    }

    public CandidateTemplate {
        elements = List.copyOf(elements);
        validateUniqueIndexes(elements);
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

    private static void validateUniqueIndexes(List<CandidateElement> elements) {
        Set<Integer> indexes = new HashSet<>();
        for (CandidateElement element : elements) {
            if (!indexes.add(element.index())) {
                throw new IllegalArgumentException("Candidate element indexes must be unique: " + element.index());
            }
        }
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
}
