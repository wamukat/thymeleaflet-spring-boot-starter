package io.github.wamukat.thymeleaflet.domain.service;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryPreview;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StoryParameterDomainService Tests")
class StoryParameterDomainServiceTest {

    @Test
    @DisplayName("型情報がないoptionsは配列として初期化される")
    void shouldGenerateArrayForOptionsWhenTypeInfoMissing() {
        StoryParameterDomainService service = new StoryParameterDomainService(storyInfo -> Map.of(), templatePath -> List.of());

        Object value = service.generateParameterValue("options", Optional.empty());

        assertThat(value).isInstanceOf(List.class);
        assertThat((List<?>) value).hasSize(2);
        assertThat((List<?>) value)
            .allSatisfy(item -> assertThat(item).isInstanceOf(Map.class));
    }

    @Test
    @DisplayName("Collection型のoptionsはvalue/labelを持つ配列で生成される")
    void shouldGenerateOptionObjectListForCollectionOptions() {
        StoryParameterDomainService service = new StoryParameterDomainService(storyInfo -> Map.of(), templatePath -> List.of());

        TypeInfo typeInfo = TypeInfo.of("options", "List<Option>", TypeInfo.TypeCategory.COLLECTION);

        Object value = service.generateParameterValue("options", Optional.of(typeInfo));

        assertThat(value).isInstanceOf(List.class);
        List<?> options = (List<?>) value;
        assertThat(options).isNotEmpty();
        assertThat(options.get(0)).isInstanceOf(Map.class);
        Map<?, ?> firstOption = (Map<?, ?>) options.get(0);
        assertThat(firstOption.containsKey("value")).isTrue();
        assertThat(firstOption.containsKey("label")).isTrue();
    }

    @Test
    @DisplayName("Map型は空のMapで初期化される")
    void shouldGenerateEmptyMapForMapType() {
        StoryParameterDomainService service = new StoryParameterDomainService(storyInfo -> Map.of(), templatePath -> List.of());

        TypeInfo typeInfo = TypeInfo.of("attributes", "Map<String,Object>", TypeInfo.TypeCategory.OBJECT);

        Object value = service.generateParameterValue("attributes", Optional.of(typeInfo));

        assertThat(value).isInstanceOf(Map.class);
        assertThat((Map<?, ?>) value).isEmpty();
    }

    @Test
    @DisplayName("generateStoryParametersはoptionsを配列として生成する")
    void shouldGenerateArrayOptionsInStoryParameters() {
        StoryParameterDomainService service = new StoryParameterDomainService(
            storyInfo -> Map.of(),
            templatePath -> List.of(TypeInfo.of("options", "Object", TypeInfo.TypeCategory.UNKNOWN))
        );

        FragmentSummary fragmentSummary = FragmentSummary.parameterized(
            "components/ui-form",
            "selectField",
            List.of("options")
        );
        StoryItem storyItem = new StoryItem("default", "Default", "", Map.of(), StoryPreview.empty(), Map.of());
        FragmentStoryInfo storyInfo = FragmentStoryInfo.of(fragmentSummary, "selectField", "default", storyItem);

        Map<String, Object> parameters = service.generateStoryParameters(storyInfo);

        assertThat(parameters).containsKey("options");
        assertThat(parameters.get("options")).isInstanceOf(List.class);
    }
}
