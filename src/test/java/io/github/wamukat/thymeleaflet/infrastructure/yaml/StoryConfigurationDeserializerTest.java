package io.github.wamukat.thymeleaflet.infrastructure.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryGroup;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StoryConfiguration直接YAML変換用ユニットテスト
 * Phase 5.0: YAML直接マッピング機能の動作確認
 */
@DisplayName("StoryConfiguration Direct YAML Mapping Tests")
class StoryConfigurationDeserializerTest {

    private ObjectMapper yamlMapper;

    @BeforeEach
    void setUp() {
        yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @Test
    @DisplayName("正常なYAMLからStoryConfigurationに直接変換できる")
    void shouldDeserializeValidYamlToStoryConfiguration() throws Exception {
        // Given
        String yamlContent = """
                meta:
                  title: "Button Component Stories"
                  description: "Various button states and variations"
                storyGroups:
                  primary:
                    title: "Primary Buttons"
                    description: "Main action buttons"
                    stories:
                      - name: "default"
                        title: "Default State"
                        description: "Standard button appearance"
                        parameters:
                          text: "Click Me"
                          variant: "primary"
                      - name: "disabled"
                        title: "Disabled State"
                        description: "Button in disabled state"
                        parameters:
                          text: "Disabled"
                          variant: "primary"
                          disabled: true
                  secondary:
                    title: "Secondary Buttons"
                    description: "Alternative action buttons"
                    stories:
                      - name: "outline"
                        title: "Outline Style"
                        description: "Outlined button style"
                        parameters:
                          text: "Outline Button"
                          variant: "outline"
                """;

        // When
        StoryConfiguration result = yamlMapper.readValue(yamlContent, StoryConfiguration.class);

        // Then
        assertThat(result).isNotNull();
        
        // Meta情報の確認
        StoryMeta meta = result.meta();
        assertThat(meta.title()).isEqualTo("Button Component Stories");
        assertThat(meta.description()).isEqualTo("Various button states and variations");
        
        // StoryGroups確認
        Map<String, StoryGroup> groups = result.storyGroups();
        assertThat(groups).hasSize(2);
        assertThat(groups).containsKeys("primary", "secondary");
        
        // Primary groupの確認
        StoryGroup primaryGroup = groups.get("primary");
        assertThat(primaryGroup.title()).isEqualTo("Primary Buttons");
        assertThat(primaryGroup.stories()).hasSize(2);
        
        StoryItem defaultStory = primaryGroup.findStoryByName("default");
        assertThat(defaultStory).isNotNull();
        assertThat(defaultStory.title()).isEqualTo("Default State");
        assertThat(defaultStory.parameters()).containsEntry("text", "Click Me");
        assertThat(defaultStory.parameters()).containsEntry("variant", "primary");
        
        // Secondary groupの確認
        StoryGroup secondaryGroup = groups.get("secondary");
        assertThat(secondaryGroup.title()).isEqualTo("Secondary Buttons");
        assertThat(secondaryGroup.stories()).hasSize(1);
        
        StoryItem outlineStory = secondaryGroup.findStoryByName("outline");
        assertThat(outlineStory).isNotNull();
        assertThat(outlineStory.parameters()).containsEntry("variant", "outline");
    }

    @Test
    @DisplayName("meta情報がnullの場合はデフォルト値で設定される")
    void shouldUseDefaultMetaWhenMetaIsNull() throws Exception {
        // Given
        String yamlContent = """
                storyGroups:
                  basic:
                    title: "Basic Group"
                    description: "Basic stories"
                    stories:
                      - name: "simple"
                        title: "Simple Story"
                        description: "Simple test story"
                """;

        // When
        StoryConfiguration result = yamlMapper.readValue(yamlContent, StoryConfiguration.class);

        // Then
        assertThat(result).isNotNull();
        StoryMeta meta = result.meta();
        assertThat(meta.title()).isEqualTo("Default Title");
        assertThat(meta.description()).isEqualTo("");
    }

    @Test
    @DisplayName("storyGroups情報がnullの場合は空のStoryGroupsが設定される")
    void shouldUseEmptyStoryGroupsWhenStoryGroupsIsNull() throws Exception {
        // Given
        String yamlContent = """
                meta:
                  title: "Empty Configuration"
                  description: "No storyGroups defined"
                """;

        // When
        StoryConfiguration result = yamlMapper.readValue(yamlContent, StoryConfiguration.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.storyGroups()).isEmpty();
        assertThat(result.getTotalStoryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("stories配列が空の場合も正常に処理される")
    void shouldHandleEmptyStoriesArray() throws Exception {
        // Given
        String yamlContent = """
                meta:
                  title: "Empty Stories"
                  description: "Group with no stories"
                storyGroups:
                  empty:
                    title: "Empty Group"
                    description: "This group has no stories"
                    stories: []
                """;

        // When
        StoryConfiguration result = yamlMapper.readValue(yamlContent, StoryConfiguration.class);

        // Then
        assertThat(result).isNotNull();
        Map<String, StoryGroup> groups = result.storyGroups();
        assertThat(groups).hasSize(1);
        
        StoryGroup emptyGroup = groups.get("empty");
        assertThat(emptyGroup).isNotNull();
        assertThat(emptyGroup.stories()).isEmpty();
        assertThat(emptyGroup.getStoryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("parameters情報がnullの場合は空のMapが設定される")
    void shouldUseEmptyParametersWhenParametersIsNull() throws Exception {
        // Given
        String yamlContent = """
                meta:
                  title: "No Parameters Story"
                  description: "Story without parameters"
                storyGroups:
                  simple:
                    title: "Simple Group"
                    description: "Simple stories"
                    stories:
                      - name: "noparams"
                        title: "No Parameters"
                        description: "Story without any parameters"
                """;

        // When
        StoryConfiguration result = yamlMapper.readValue(yamlContent, StoryConfiguration.class);

        // Then
        assertThat(result).isNotNull();
        StoryGroup group = result.getStoryGroup("simple");
        StoryItem story = group.findStoryByName("noparams");
        
        assertThat(story).isNotNull();
        assertThat(story.parameters()).isEmpty();
        assertThat(story.hasParameters()).isFalse();
    }

    @Test
    @DisplayName("不正なYAMLの場合は例外が発生する")
    void shouldThrowExceptionForInvalidYaml() throws Exception {
        // Given - 不正なYAML
        String invalidYamlContent = """
                meta:
                  title: "Invalid YAML
                  description: Missing quote
                storyGroups:
                  invalid: [
                """;

        // When & Then
        assertThatThrownBy(() -> yamlMapper.readValue(invalidYamlContent, StoryConfiguration.class))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("複雑なparameters構造も正常に処理される")
    void shouldHandleComplexParametersStructure() throws Exception {
        // Given
        String yamlContent = """
                meta:
                  title: "Complex Parameters"
                  description: "Story with complex parameter structure"
                storyGroups:
                  advanced:
                    title: "Advanced Group"
                    description: "Advanced parameter stories"
                    stories:
                      - name: "complex"
                        title: "Complex Parameters"
                        description: "Story with nested parameters"
                        parameters:
                          text: "Complex Button"
                          style:
                            color: "blue"
                            size: "large"
                            border:
                              width: 2
                              style: "solid"
                          features:
                            - "clickable"
                            - "hoverable"
                            - "focusable"
                """;

        // When
        StoryConfiguration result = yamlMapper.readValue(yamlContent, StoryConfiguration.class);

        // Then
        assertThat(result).isNotNull();
        StoryGroup group = result.getStoryGroup("advanced");
        StoryItem story = group.findStoryByName("complex");
        
        assertThat(story).isNotNull();
        assertThat(story.parameters()).containsKey("text");
        assertThat(story.parameters()).containsKey("style");
        assertThat(story.parameters()).containsKey("features");
        
        // 複雑な構造の値も取得できることを確認
        Object style = story.parameters().get("style");
        assertThat(style).isNotNull();
    }
}