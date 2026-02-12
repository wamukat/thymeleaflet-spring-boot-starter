package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.domain.service.TemplateModelExpressionAnalyzer;
import io.github.wamukat.thymeleaflet.domain.service.ModelValueInferenceService;
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
            new TemplateModelExpressionAnalyzer(),
            new ModelValueInferenceService()
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
        assertThat(row.get("amount")).isEqualTo(0);
        assertThat(row.get("balanceAfter")).isEqualTo(0);
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
}
