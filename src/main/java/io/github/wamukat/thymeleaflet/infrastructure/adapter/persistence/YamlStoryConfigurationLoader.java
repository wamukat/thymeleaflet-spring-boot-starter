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
        if (templatePath == null || templatePath.trim().isEmpty()) {
            logger.debug("Template path is null or empty");
            return Optional.empty();
        }

        String storyFilePath = STORY_BASE_PATH + templatePath + ".stories.yml";
        
        try {
            Resource resource = resourceLoader.getResource(storyFilePath);
            if (!resource.exists()) {
                logger.debug("Story file not found: {}", storyFilePath);
                return Optional.empty();
            }

            try (InputStream inputStream = resource.getInputStream()) {
                StoryConfiguration config = yamlMapper.readValue(inputStream, StoryConfiguration.class);
                
                if (config != null) {
                    logger.debug("Successfully loaded story configuration from: {}", storyFilePath);
                    return Optional.of(config);
                } else {
                    logger.warn("YAML parsing resulted in null configuration: {}", storyFilePath);
                    return Optional.empty();
                }
            }

        } catch (IOException e) {
            logger.warn("Failed to load story file {}: {}", storyFilePath, e.getMessage(), e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error loading story file {}: {}", storyFilePath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Story設定ファイルの存在確認
     * Infrastructure技術的責任: ファイルシステム操作
     */
    public boolean storyConfigurationExists(String templatePath) {
        if (templatePath == null || templatePath.trim().isEmpty()) {
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
        if (templatePath == null || templatePath.trim().isEmpty()) {
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
}