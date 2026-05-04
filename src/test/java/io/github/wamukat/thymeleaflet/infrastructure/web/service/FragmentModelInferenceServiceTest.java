package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.domain.service.TemplateModelExpressionAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class FragmentModelInferenceServiceTest {

    private final FragmentModelInferenceService service =
        new FragmentModelInferenceService(
            new DefaultResourceLoader(),
            new TemplateModelExpressionAnalyzer()
        );

    @Test
    void shouldInferModelFromTemplateExpressions() {
        Map<String, Object> inferred = service.inferModel(
            "fragments/model-inference-sample",
            "modelInferenceSample",
            List.of("label")
        );

        assertThat(inferred).containsKeys("displayName", "isActive", "member");
        assertThat(inferred.get("displayName")).isEqualTo("Sample displayName");
        assertThat(inferred.get("isActive")).isEqualTo(false);
        assertThat(inferred).doesNotContainKey("label");

        assertThat(inferred).containsEntry("member", Map.of("status", "Sample status"));
    }

    @Test
    void shouldIgnoreParametersLocalVariablesAndUtilityObjects() {
        Map<String, Object> inferred = service.inferModel(
            "components/ui-button-inference-sample",
            "submitButton",
            List.of("label", "styleType")
        );

        assertThat(inferred).isEmpty();
    }

    @Test
    void shouldInferCollectionItemStructureFromThEach() {
        Map<String, Object> inferred = service.inferModel(
            "fragments/points-panel-inference-sample",
            "pointsPanel",
            List.of()
        );

        assertThat(inferred).containsKey("pointPage");
        assertThat(inferred).doesNotContainKey("row");

        @SuppressWarnings("unchecked")
        Map<String, Object> pointPage = (Map<String, Object>) Objects.requireNonNull(inferred.get("pointPage"));
        assertThat(pointPage).containsKey("items");
        assertThat(pointPage.get("items")).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) Objects.requireNonNull(pointPage.get("items"));
        assertThat(items).isNotEmpty();
        assertThat(items.get(0)).isInstanceOf(LinkedHashMap.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) items.get(0);
        assertThat(row).containsKeys("occurredAt", "description", "amount", "balanceAfter");
        assertThat(row).doesNotContainKey("index");
        assertThat(row.get("amount")).isEqualTo(0);
        assertThat(row.get("balanceAfter")).isEqualTo(0);
    }

    @Test
    void shouldInferFormFieldsFromThObjectAndThField() {
        Map<String, Object> inferred = service.inferModel(
            "fragments/form-object-inference-sample",
            "profileForm",
            List.of()
        );

        assertThat(inferred).containsKey("profileForm");
        assertThat(inferred).doesNotContainKeys("email", "enabled", "address");

        @SuppressWarnings("unchecked")
        Map<String, Object> profileForm =
            (Map<String, Object>) Objects.requireNonNull(inferred.get("profileForm"));
        assertThat(profileForm)
            .containsEntry("email", "sample@example.com")
            .containsEntry("enabled", false)
            .containsEntry("address", Map.of("city", "Sample city"));
    }

    @Test
    void shouldNotInferFormFieldsWhenThObjectRootIsAParameter() {
        Map<String, Object> inferred = service.inferModel(
            "fragments/form-object-inference-sample",
            "profileForm",
            List.of("profileForm")
        );

        assertThat(inferred).isEmpty();
    }

    @Test
    void shouldMergeModelRequirementsFromStaticThReplaceReference() {
        Map<String, Object> inferred = service.inferModel(
            "fragments/points-content-inference-sample",
            "pointsContent",
            List.of()
        );

        assertThat(inferred).containsKey("pointPage");

        @SuppressWarnings("unchecked")
        Map<String, Object> pointPage = (Map<String, Object>) Objects.requireNonNull(inferred.get("pointPage"));
        assertThat(pointPage).containsKeys("totalPoints", "expiringPoints", "pageSize", "totalItems", "items");

        assertThat(inferred).containsKeys(
            "selectedFilter",
            "currentPage",
            "totalPages",
            "hasPrev",
            "prevPage",
            "hasNext",
            "nextPage"
        );
    }

    @Test
    void shouldNotMergeChildModelWhenStaticReferenceUsesLiteralArgumentsOnly() {
        Map<String, Object> inferred = service.inferModel(
            "fragments/literal-child-reference-inference-sample",
            "literalChildReferenceInferenceSample",
            List.of("title")
        );

        assertThat(inferred).doesNotContainKeys("label", "variant");
    }

    @Test
    void shouldMapNamedFragmentArgumentsToChildDeclarationParameters() {
        Map<String, Object> inferred = service.inferModel(
            "fragments/named-child-reference-inference-sample",
            "namedChildReferenceInferenceSample",
            List.of()
        );

        assertThat(inferred).containsKeys("view", "childOnly");
        assertThat(inferred).doesNotContainKeys("title", "variant");

        @SuppressWarnings("unchecked")
        Map<String, Object> view = (Map<String, Object>) Objects.requireNonNull(inferred.get("view"));
        assertThat(view).containsKeys("title", "variant");
    }

    @Test
    void shouldAvoidDeclarationAwareMappingForMixedFragmentArguments() {
        Map<String, Object> inferred = service.inferModel(
            "fragments/mixed-child-reference-inference-sample",
            "mixedChildReferenceInferenceSample",
            List.of()
        );

        assertThat(inferred).containsKeys("view", "title", "variant", "childOnly");
    }

    @Test
    void shouldMapNamedFragmentArgumentsWhenInferringMethodReturnCandidates() {
        Map<String, Object> inferred = service.inferMethodReturnCandidates(
            "fragments/named-child-reference-inference-sample",
            "namedChildReferenceInferenceSample",
            List.of()
        );

        assertThat(inferred).containsKey("childMethodOnly");
        assertThat(inferred).doesNotContainKeys("title", "variant");
    }

    @Test
    void shouldInferIndexedNoArgMethodReturnCandidatesAsListModel() {
        Map<String, Object> inferred = service.inferMethodReturnCandidates(
            "fragments/indexed-method-inference-sample",
            "indexedMethodInferenceSample",
            List.of()
        );

        assertThat(inferred).containsEntry("view", Map.of(
            "items", List.of(Map.of("hasVisible", false))
        ));
    }
}
