package io.github.wamukat.thymeleaflet.infrastructure.adapter.persistence;

import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * YamlStoryConfigurationLoader用ユニットテスト
 * Phase 5.1: Infrastructure層YAML設定読み込み機能のテスト
 */
@DisplayName("YamlStoryConfigurationLoader Tests")
class YamlStoryConfigurationLoaderTest {

    private YamlStoryConfigurationLoader loader;

    @Mock
    private ResourceLoader mockResourceLoader;
    
    @Mock
    private Resource mockResource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loader = new YamlStoryConfigurationLoader(mockResourceLoader);
    }

    @Test
    @DisplayName("有効なStory設定を正常に読み込める")
    void shouldLoadValidStoryConfiguration() throws IOException {
        // Given
        String templatePath = "templates/domain/components/button";
        String yamlContent = """
            stories:
              primary-button:
                title: "Primary Button"
                parameters:
                  text: "Click me"
                  variant: "primary"
              secondary-button:
                title: "Secondary Button" 
                parameters:
                  text: "Cancel"
                  variant: "secondary"
            """;
        
        when(mockResourceLoader.getResource(any(String.class))).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(true);
        when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));

        // When
        Optional<StoryConfiguration> result = loader.loadStoryConfiguration(templatePath);

        // Then
        assertThat(result).isPresent();
        StoryConfiguration config = result.get();
        assertThat(config.storyGroups()).isNotNull();
    }

    @Test
    @DisplayName("preview.viewportをStoryPreview設定として読み込める")
    void shouldLoadStoryPreviewViewport() throws IOException {
        // Given
        String templatePath = "templates/domain/components/responsive-header";
        String yamlContent = """
            storyGroups:
              responsiveHeader:
                title: "Responsive Header"
                stories:
                  - name: default
                    preview:
                      viewport: mobileSmall
                      wrapper: |
                        <div>{{content}}</div>
            """;

        when(mockResourceLoader.getResource(any(String.class))).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(true);
        when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));

        // When
        Optional<StoryConfiguration> result = loader.loadStoryConfiguration(templatePath);

        // Then
        assertThat(result).isPresent();
        StoryGroup storyGroup = Objects.requireNonNull(
            result.orElseThrow().storyGroups().get("responsiveHeader"));
        assertThat(storyGroup.stories().get(0).preview().viewport())
            .isEqualTo("mobileSmall");
    }

    @Test
    @DisplayName("preview.minHeightをStoryPreview設定として読み込める")
    void shouldLoadStoryPreviewMinHeight() throws IOException {
        // Given
        String templatePath = "templates/domain/components/fixed-toolbar";
        String yamlContent = """
            storyGroups:
              fixedToolbar:
                title: "Fixed Toolbar"
                stories:
                  - name: default
                    preview:
                      minHeight: 96
            """;

        when(mockResourceLoader.getResource(any(String.class))).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(true);
        when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes()));

        // When
        Optional<StoryConfiguration> result = loader.loadStoryConfiguration(templatePath);

        // Then
        assertThat(result).isPresent();
        StoryGroup storyGroup = Objects.requireNonNull(
            result.orElseThrow().storyGroups().get("fixedToolbar"));
        assertThat(storyGroup.stories().get(0).preview().minHeight())
            .isEqualTo(96);
    }

    @Test
    @DisplayName("存在しないファイルの場合はOptional.emptyを返す")
    void shouldReturnEmptyForNonexistentFile() throws IOException {
        // Given
        String templatePath = "templates/nonexistent/component";
        
        when(mockResourceLoader.getResource(any(String.class))).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(false);

        // When
        Optional<StoryConfiguration> result = loader.loadStoryConfiguration(templatePath);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("不正なYAMLの場合はOptional.emptyを返す")
    void shouldReturnEmptyForInvalidYaml() throws IOException {
        // Given
        String templatePath = "templates/invalid/component";
        String invalidYamlContent = """
            stories
              - invalid yaml structure
            """;
        
        when(mockResourceLoader.getResource(any(String.class))).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(true);
        when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(invalidYamlContent.getBytes()));

        // When
        Optional<StoryConfiguration> result = loader.loadStoryConfiguration(templatePath);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("存在しないYAMLはmissing診断として扱う")
    void shouldReturnMissingResultForNonexistentFile() throws IOException {
        // Given
        String templatePath = "templates/nonexistent/component";

        when(mockResourceLoader.getResource(any(String.class))).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(false);

        // When
        YamlStoryConfigurationLoader.StoryConfigurationLoadResult result =
            loader.loadStoryConfigurationWithDiagnostics(templatePath);

        // Then
        assertThat(result.status()).isEqualTo(YamlStoryConfigurationLoader.StoryConfigurationLoadStatus.MISSING);
        assertThat(result.configuration()).isEmpty();
        assertThat(result.diagnostic()).isEmpty();
    }

    @Test
    @DisplayName("存在するYAMLの読み込み失敗は診断情報として保持する")
    void shouldReturnDiagnosticForExistingYamlLoadFailure() throws IOException {
        // Given
        String templatePath = "templates/broken/component";

        when(mockResourceLoader.getResource(any(String.class))).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(true);
        when(mockResource.getInputStream()).thenThrow(new IOException("broken yaml stream"));

        // When
        YamlStoryConfigurationLoader.StoryConfigurationLoadResult result =
            loader.loadStoryConfigurationWithDiagnostics(templatePath);

        // Then
        assertThat(result.status()).isEqualTo(YamlStoryConfigurationLoader.StoryConfigurationLoadStatus.FAILED);
        assertThat(result.configuration()).isEmpty();
        assertThat(result.diagnostic()).isPresent();
        YamlStoryConfigurationLoader.StoryConfigurationDiagnostic diagnostic = result.diagnostic().orElseThrow();
        assertThat(diagnostic.code()).isEqualTo("STORY_YAML_LOAD_FAILED");
        assertThat(diagnostic.storyFilePath()).contains(templatePath + ".stories.yml");
        assertThat(diagnostic.userSafeMessage()).contains("could not be loaded or parsed");
        assertThat(diagnostic.developerMessage()).contains("broken yaml stream");
    }

    @Test
    @DisplayName("nullのtemplatePathの場合はNullPointerExceptionを送出する")
    @SuppressWarnings("NullAway")
    void shouldHandleNullTemplatePath() {
        assertThatThrownBy(() -> loader.loadStoryConfiguration(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Story設定ファイルの存在確認が正常に動作する")
    @SuppressWarnings("NullAway")
    void shouldCheckStoryConfigurationExists() {
        // Given
        String existingTemplatePath = "templates/existing/component";
        String nonExistingTemplatePath = "templates/nonexisting/component";
        
        when(mockResourceLoader.getResource("classpath:META-INF/thymeleaflet/stories/" + existingTemplatePath + ".stories.yml"))
            .thenReturn(mockResource);
        when(mockResourceLoader.getResource("classpath:META-INF/thymeleaflet/stories/" + nonExistingTemplatePath + ".stories.yml"))
            .thenReturn(mockResource);
        
        when(mockResource.exists())
            .thenReturn(true)   // for existing
            .thenReturn(false); // for non-existing

        // When & Then
        assertThat(loader.storyConfigurationExists(existingTemplatePath)).isTrue();
        assertThat(loader.storyConfigurationExists(nonExistingTemplatePath)).isFalse();
        assertThatThrownBy(() -> loader.storyConfigurationExists(null))
            .isInstanceOf(NullPointerException.class);
        assertThat(loader.storyConfigurationExists("")).isFalse();
    }

    @Test
    @DisplayName("最終更新時刻取得が正常に動作する")
    @SuppressWarnings("NullAway")
    void shouldGetLastModifiedTime() throws IOException {
        // Given
        String templatePath = "templates/domain/components/button";
        long expectedTime = System.currentTimeMillis();
        
        when(mockResourceLoader.getResource(any(String.class))).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(true);
        when(mockResource.lastModified()).thenReturn(expectedTime);

        // When
        Optional<Long> result = loader.getStoryConfigurationLastModified(templatePath);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedTime);
        
        assertThatThrownBy(() -> loader.getStoryConfigurationLastModified(null))
            .isInstanceOf(NullPointerException.class);
    }
}
