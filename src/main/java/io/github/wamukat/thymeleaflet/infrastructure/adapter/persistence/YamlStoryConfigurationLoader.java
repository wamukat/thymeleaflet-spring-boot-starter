package io.github.wamukat.thymeleaflet.infrastructure.adapter.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * YAML Story設定ファイル読み込み専用Infrastructure実装
 * 
 * Phase 5.1: StoryManagementUseCaseからYAML読み込み責任を抽出
 * Pure Infrastructure責任: YAML技術的読み込み処理のみ
 */
@Component
public class YamlStoryConfigurationLoader {

    private static final Logger logger = LoggerFactory.getLogger(YamlStoryConfigurationLoader.class);
    private static final String STORY_BASE_PATH = "classpath:META-INF/thymeleaflet/stories/";

    private final ResourceLoader resourceLoader;
    private final ObjectMapper yamlMapper;

    {
        yamlMapper = new ObjectMapper(new YAMLFactory());
        // Record対応設定
        yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        yamlMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    }

    public YamlStoryConfigurationLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 指定されたテンプレートパスに対応するStory設定をYAMLから読み込み
     * Infrastructure技術的責任: ファイル読み込み・YAML解析のみ
     */
    public Optional<StoryConfiguration> loadStoryConfiguration(String templatePath) {
        return loadStoryConfigurationWithDiagnostics(templatePath).configuration();
    }

    /**
     * Story設定を読み込み、未定義fallbackと読み込み/解析失敗を区別できる結果を返す。
     */
    public StoryConfigurationLoadResult loadStoryConfigurationWithDiagnostics(String templatePath) {
        if (templatePath.isBlank()) {
            logger.debug("Template path is empty");
            return StoryConfigurationLoadResult.missing("");
        }

        String storyFilePath = STORY_BASE_PATH + templatePath + ".stories.yml";
        
        try {
            Resource resource = resourceLoader.getResource(storyFilePath);
            if (!resource.exists()) {
                logger.debug("Story file not found: {}", storyFilePath);
                return StoryConfigurationLoadResult.missing(storyFilePath);
            }

            try (InputStream inputStream = resource.getInputStream()) {
                StoryConfiguration config = yamlMapper.readValue(inputStream, StoryConfiguration.class);

                Optional<StoryConfiguration> configuration = Optional.ofNullable(config);
                if (configuration.isEmpty()) {
                    logger.warn("YAML parsing resulted in null configuration: {}", storyFilePath);
                    return StoryConfigurationLoadResult.failure(StoryConfigurationDiagnostic.nullConfiguration(storyFilePath));
                } else {
                    logger.debug("Successfully loaded story configuration from: {}", storyFilePath);
                }
                return StoryConfigurationLoadResult.loaded(configuration.orElseThrow());
            }

        } catch (IOException e) {
            StoryConfigurationDiagnostic diagnostic = StoryConfigurationDiagnostic.loadFailed(storyFilePath, e);
            logger.warn("{}: {}", diagnostic.userSafeMessage(), diagnostic.developerMessage());
            return StoryConfigurationLoadResult.failure(diagnostic);
        } catch (Exception e) {
            StoryConfigurationDiagnostic diagnostic = StoryConfigurationDiagnostic.unexpectedFailure(storyFilePath, e);
            logger.error("{}: {}", diagnostic.userSafeMessage(), diagnostic.developerMessage(), e);
            return StoryConfigurationLoadResult.failure(diagnostic);
        }
    }

    /**
     * Story設定ファイルの存在確認
     * Infrastructure技術的責任: ファイルシステム操作
     */
    public boolean storyConfigurationExists(String templatePath) {
        if (templatePath.isBlank()) {
            return false;
        }

        String storyFilePath = STORY_BASE_PATH + templatePath + ".stories.yml";
        
        try {
            Resource resource = resourceLoader.getResource(storyFilePath);
            return resource.exists();
        } catch (Exception e) {
            logger.debug("Error checking story file existence {}: {}", storyFilePath, e.getMessage());
            return false;
        }
    }

    /**
     * Story設定ファイルの最終更新時刻取得
     * Infrastructure技術的責任: ファイルシステム操作
     */
    public Optional<Long> getStoryConfigurationLastModified(String templatePath) {
        if (templatePath.isBlank()) {
            return Optional.empty();
        }

        String storyFilePath = STORY_BASE_PATH + templatePath + ".stories.yml";
        
        try {
            Resource resource = resourceLoader.getResource(storyFilePath);
            if (!resource.exists()) {
                return Optional.empty();
            }

            long lastModified = resource.lastModified();
            return Optional.of(lastModified);

        } catch (Exception e) {
            logger.debug("Error getting last modified time for {}: {}", storyFilePath, e.getMessage());
            return Optional.empty();
        }
    }

    public enum StoryConfigurationLoadStatus {
        LOADED,
        MISSING,
        FAILED
    }

    public record StoryConfigurationDiagnostic(
        String code,
        String storyFilePath,
        String userSafeMessage,
        String developerMessage
    ) {
        private static StoryConfigurationDiagnostic nullConfiguration(String storyFilePath) {
            return new StoryConfigurationDiagnostic(
                "STORY_YAML_NULL_CONFIGURATION",
                storyFilePath,
                "Story YAML was found but did not produce a valid configuration.",
                "YAML parser returned null configuration for " + storyFilePath
            );
        }

        private static StoryConfigurationDiagnostic loadFailed(String storyFilePath, IOException exception) {
            return new StoryConfigurationDiagnostic(
                "STORY_YAML_LOAD_FAILED",
                storyFilePath,
                "Story YAML was found but could not be loaded or parsed.",
                exception.getClass().getSimpleName() + ": " + exception.getMessage()
            );
        }

        private static StoryConfigurationDiagnostic unexpectedFailure(String storyFilePath, Exception exception) {
            return new StoryConfigurationDiagnostic(
                "STORY_YAML_UNEXPECTED_FAILURE",
                storyFilePath,
                "Story YAML was found but an unexpected error occurred while loading it.",
                exception.getClass().getSimpleName() + ": " + exception.getMessage()
            );
        }
    }

    public record StoryConfigurationLoadResult(
        StoryConfigurationLoadStatus status,
        Optional<StoryConfiguration> configuration,
        Optional<StoryConfigurationDiagnostic> diagnostic
    ) {
        private static StoryConfigurationLoadResult loaded(StoryConfiguration configuration) {
            return new StoryConfigurationLoadResult(
                StoryConfigurationLoadStatus.LOADED,
                Optional.of(configuration),
                Optional.empty()
            );
        }

        private static StoryConfigurationLoadResult missing(String storyFilePath) {
            return new StoryConfigurationLoadResult(
                StoryConfigurationLoadStatus.MISSING,
                Optional.empty(),
                Optional.empty()
            );
        }

        private static StoryConfigurationLoadResult failure(StoryConfigurationDiagnostic diagnostic) {
            return new StoryConfigurationLoadResult(
                StoryConfigurationLoadStatus.FAILED,
                Optional.empty(),
                Optional.of(diagnostic)
            );
        }
    }
}
