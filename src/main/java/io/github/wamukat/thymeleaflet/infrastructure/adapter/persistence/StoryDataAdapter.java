package io.github.wamukat.thymeleaflet.infrastructure.adapter.persistence;

import io.github.wamukat.thymeleaflet.application.port.outbound.StoryDataPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryGroup;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryPreview;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Collections;
import java.util.Map;

/**
 * ストーリーデータアクセスアダプタ
 * 
 * StoryDataPortの実装として、YAML設定やファイルシステムアクセスを提供
 */
@Component
public class StoryDataAdapter implements StoryDataPort {
    
    private static final Logger logger = LoggerFactory.getLogger(StoryDataAdapter.class);

    private final YamlStoryConfigurationLoader yamlStoryConfigurationLoader;
    
    @Autowired
    private FragmentDiscoveryService fragmentDiscoveryService;
    
    @Autowired
    private FragmentSummaryMapper fragmentSummaryMapper;
    
    public StoryDataAdapter(YamlStoryConfigurationLoader yamlStoryConfigurationLoader) {
        this.yamlStoryConfigurationLoader = yamlStoryConfigurationLoader;
    }
    
    @Override
    public @Nullable StoryConfiguration loadStoryConfiguration(String templatePath) {
        try {
            return yamlStoryConfigurationLoader.loadStoryConfiguration(templatePath)
                .orElse(null);
        } catch (Exception e) {
            logger.error("Failed to load story configuration for {}: {}", templatePath, e.getMessage());
            return null;
        }
    }
    
    @Override
    public Map<String, Object> loadStoryParameters(FragmentStoryInfo storyInfo) {
        Map<String, Object> parameters = new HashMap<>();
        
        try {
            StoryConfiguration config = loadStoryConfiguration(
                storyInfo.getFragmentSummary().getTemplatePath());
            
            if (config != null && config.storyGroups() != null) {
                String fragmentName = storyInfo.getFragmentSummary().getFragmentName();
                String storyName = storyInfo.getStoryName();
                
                // フラグメント名に対応するStoryGroupを取得
                StoryGroup fragmentGroup = config.storyGroups().get(fragmentName);
                
                if (fragmentGroup != null && fragmentGroup.stories() != null) {
                    // 指定されたストーリー名に対応するストーリーを検索
                    fragmentGroup.stories().stream()
                        .filter(story -> storyName.equals(story.name()))
                        .findFirst()
                        .ifPresent(story -> {
                            if (story.parameters() != null) {
                                parameters.putAll(story.parameters());
                                logger.debug("Loaded story parameters for {}::{}: {}", 
                                           fragmentName, storyName, parameters);
                            }
                        });
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to load story parameters for {}: {}", 
                        storyInfo.getFragmentSummary().getFragmentName(), e.getMessage());
        }
        
        return parameters;
    }

    @Override
    public @Nullable FragmentStoryInfo getStory(String templatePath, String fragmentName, String storyName) {
        try {
            StoryConfiguration storyConfiguration = loadStoryConfiguration(templatePath);
            
            // Story configuration が見つからない場合はデフォルトストーリーを返す
            if (storyConfiguration == null) {
                logger.debug("Story configuration not found for template: {}, returning default story", templatePath);
                return createDefaultStory(templatePath, fragmentName, storyName);
            }
            
            // StoryConfiguration.storyGroups()はMap<String, StoryGroup>を返す
            StoryGroup storyGroup = storyConfiguration.storyGroups().get(fragmentName);
            if (storyGroup == null) {
                logger.debug("Story group not found for fragment: {}, returning default story", fragmentName);
                return createDefaultStory(templatePath, fragmentName, storyName);
            }
            
            // Custom story: base storyから作成して返す
            if ("custom".equals(storyName)) {
                StoryItem baseStory = resolveBaseStory(storyGroup);
                if (baseStory == null) {
                    logger.debug("Base story not found for custom: {}::{}", templatePath, fragmentName);
                    return createDefaultStory(templatePath, fragmentName, storyName);
                }

                // FragmentDiscoveryServiceから実際のFragmentInfoを取得
                FragmentDiscoveryService.FragmentInfo fragmentInfo = fragmentDiscoveryService.discoverFragments()
                    .stream()
                    .filter(f -> f.getTemplatePath().equals(templatePath) && f.getFragmentName().equals(fragmentName))
                    .findFirst()
                    .orElse(null);
                if (fragmentInfo == null) {
                    logger.warn("FragmentInfo not found for {}::{}", templatePath, fragmentName);
                    return null;
                }

                StoryItem customStory = new StoryItem(
                    "custom",
                    "Custom",
                    "",
                    baseStory.parameters(),
                    baseStory.preview(),
                    baseStory.model()
                );

                io.github.wamukat.thymeleaflet.domain.model.FragmentSummary domainFragmentSummary =
                    fragmentSummaryMapper.toDomain(fragmentInfo);
                return FragmentStoryInfo.of(
                    domainFragmentSummary,
                    fragmentName,
                    "custom",
                    customStory
                );
            }

            // StoryGroup.findStoryByName()を使用してストーリーを検索
            StoryItem storyItem = storyGroup.findStoryByName(storyName);
            if (storyItem == null) {
                logger.debug("Story not found: {}::{}::{}, returning default story", templatePath, fragmentName, storyName);
                return createDefaultStory(templatePath, fragmentName, storyName);
            }
            
            // StoryItemをFragmentStoryInfoに変換
            // FragmentDiscoveryServiceから実際のFragmentInfoを取得
            FragmentDiscoveryService.FragmentInfo fragmentInfo = fragmentDiscoveryService.discoverFragments()
                .stream()
                .filter(f -> f.getTemplatePath().equals(templatePath) && f.getFragmentName().equals(fragmentName))
                .findFirst()
                .orElse(null);
            
            if (fragmentInfo == null) {
                logger.warn("FragmentInfo not found for {}::{}", templatePath, fragmentName);
                return null;
            }
            
            io.github.wamukat.thymeleaflet.domain.model.FragmentSummary domainFragmentSummary = 
                fragmentSummaryMapper.toDomain(fragmentInfo);
            return FragmentStoryInfo.of(
                domainFragmentSummary,
                fragmentName,
                storyName,
                storyItem
            );
            
        } catch (Exception e) {
            logger.error("Error retrieving story: {}::{}::{}", templatePath, fragmentName, storyName, e);
            return null;
        }
    }
    
    /**
     * デフォルトストーリーを作成
     */
    private @Nullable FragmentStoryInfo createDefaultStory(String templatePath, String fragmentName, String storyName) {
        // FragmentDiscoveryServiceから実際のFragmentInfoを取得
        FragmentDiscoveryService.FragmentInfo fragmentInfo = fragmentDiscoveryService.discoverFragments()
            .stream()
            .filter(f -> f.getTemplatePath().equals(templatePath) && f.getFragmentName().equals(fragmentName))
            .findFirst()
            .orElse(null);
        
        if (fragmentInfo == null) {
            logger.debug("FragmentInfo not found for {}::{}, cannot create default story", templatePath, fragmentName);
            return null;
        }

        String resolvedStoryName = (storyName != null && !storyName.isBlank()) ? storyName : "default";
        StoryItem defaultStory = new StoryItem(
            resolvedStoryName,
            resolvedStoryName,
            "",
            Collections.emptyMap(),
            StoryPreview.empty(),
            Collections.emptyMap()
        );
        
        io.github.wamukat.thymeleaflet.domain.model.FragmentSummary domainFragmentSummary = 
            fragmentSummaryMapper.toDomain(fragmentInfo);
        return FragmentStoryInfo.of(
            domainFragmentSummary,
            fragmentName,
            resolvedStoryName,
            defaultStory
        );
    }

    private @Nullable StoryItem resolveBaseStory(StoryGroup storyGroup) {
        if (storyGroup == null || storyGroup.stories().isEmpty()) {
            return null;
        }
        StoryItem defaultStory = storyGroup.findStoryByName("default");
        if (defaultStory != null) {
            return defaultStory;
        }
        return storyGroup.stories().get(0);
    }
}
