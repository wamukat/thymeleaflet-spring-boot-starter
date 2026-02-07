package io.github.wamukat.thymeleaflet.infrastructure.adapter.persistence;

import io.github.wamukat.thymeleaflet.application.port.outbound.StoryDataPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryGroup;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryPreview;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

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
    public Optional<StoryConfiguration> loadStoryConfiguration(String templatePath) {
        try {
            return yamlStoryConfigurationLoader.loadStoryConfiguration(templatePath);
        } catch (Exception e) {
            logger.error("Failed to load story configuration for {}: {}", templatePath, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public Map<String, Object> loadStoryParameters(FragmentStoryInfo storyInfo) {
        Map<String, Object> parameters = new HashMap<>();
        
        try {
            StoryConfiguration config = loadStoryConfiguration(
                storyInfo.getFragmentSummary().getTemplatePath()
            ).orElse(null);
            
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
    public Optional<FragmentStoryInfo> getStory(String templatePath, String fragmentName, String storyName) {
        try {
            Optional<StoryConfiguration> storyConfiguration = loadStoryConfiguration(templatePath);
            
            // Story configuration が見つからない場合はデフォルトストーリーを返す
            if (storyConfiguration.isEmpty()) {
                logger.debug("Story configuration not found for template: {}, returning default story", templatePath);
                return createDefaultStory(templatePath, fragmentName, storyName);
            }
            
            // StoryConfiguration.storyGroups()はMap<String, StoryGroup>を返す
            StoryGroup storyGroup = storyConfiguration.get().storyGroups().get(fragmentName);
            if (storyGroup == null) {
                logger.debug("Story group not found for fragment: {}, returning default story", fragmentName);
                return createDefaultStory(templatePath, fragmentName, storyName);
            }
            
            // Custom story: base storyから作成して返す
            if ("custom".equals(storyName)) {
                Optional<StoryItem> baseStory = resolveBaseStory(storyGroup);
                if (baseStory.isEmpty()) {
                    logger.debug("Base story not found for custom: {}::{}", templatePath, fragmentName);
                    return createDefaultStory(templatePath, fragmentName, storyName);
                }

                // FragmentDiscoveryServiceから実際のFragmentInfoを取得
                Optional<FragmentDiscoveryService.FragmentInfo> fragmentInfo = findFragmentInfo(templatePath, fragmentName);
                if (fragmentInfo.isEmpty()) {
                    logger.warn("FragmentInfo not found for {}::{}", templatePath, fragmentName);
                    return Optional.empty();
                }

                StoryItem customStory = new StoryItem(
                    "custom",
                    "Custom",
                    "",
                    baseStory.get().parameters(),
                    baseStory.get().preview(),
                    baseStory.get().model()
                );

                io.github.wamukat.thymeleaflet.domain.model.FragmentSummary domainFragmentSummary =
                    fragmentSummaryMapper.toDomain(fragmentInfo.get());
                return Optional.of(FragmentStoryInfo.of(
                    domainFragmentSummary,
                    fragmentName,
                    "custom",
                    customStory
                ));
            }

            // StoryGroup.findStoryByName()を使用してストーリーを検索
            Optional<StoryItem> storyItemOptional = storyGroup.findStoryByName(storyName);
            if (storyItemOptional.isEmpty()) {
                logger.debug("Story not found: {}::{}::{}, returning default story", templatePath, fragmentName, storyName);
                return createDefaultStory(templatePath, fragmentName, storyName);
            }
            StoryItem storyItem = storyItemOptional.orElseThrow();
            
            // StoryItemをFragmentStoryInfoに変換
            // FragmentDiscoveryServiceから実際のFragmentInfoを取得
            Optional<FragmentDiscoveryService.FragmentInfo> fragmentInfo = findFragmentInfo(templatePath, fragmentName);
            if (fragmentInfo.isEmpty()) {
                logger.warn("FragmentInfo not found for {}::{}", templatePath, fragmentName);
                return Optional.empty();
            }
            
            io.github.wamukat.thymeleaflet.domain.model.FragmentSummary domainFragmentSummary = 
                fragmentSummaryMapper.toDomain(fragmentInfo.get());
            return Optional.of(FragmentStoryInfo.of(
                domainFragmentSummary,
                fragmentName,
                storyName,
                storyItem
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving story: {}::{}::{}", templatePath, fragmentName, storyName, e);
            return Optional.empty();
        }
    }
    
    /**
     * デフォルトストーリーを作成
     */
    private Optional<FragmentStoryInfo> createDefaultStory(String templatePath, String fragmentName, String storyName) {
        // FragmentDiscoveryServiceから実際のFragmentInfoを取得
        Optional<FragmentDiscoveryService.FragmentInfo> fragmentInfo = findFragmentInfo(templatePath, fragmentName);
        
        if (fragmentInfo.isEmpty()) {
            logger.debug("FragmentInfo not found for {}::{}, cannot create default story", templatePath, fragmentName);
            return Optional.empty();
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
            fragmentSummaryMapper.toDomain(fragmentInfo.get());
        return Optional.of(FragmentStoryInfo.of(
            domainFragmentSummary,
            fragmentName,
            resolvedStoryName,
            defaultStory
        ));
    }

    private Optional<StoryItem> resolveBaseStory(StoryGroup storyGroup) {
        if (storyGroup == null || storyGroup.stories().isEmpty()) {
            return Optional.empty();
        }
        Optional<StoryItem> defaultStory = storyGroup.findStoryByName("default");
        if (defaultStory.isPresent()) {
            return defaultStory;
        }
        return Optional.of(storyGroup.stories().get(0));
    }

    private Optional<FragmentDiscoveryService.FragmentInfo> findFragmentInfo(String templatePath, String fragmentName) {
        return fragmentDiscoveryService.discoverFragments()
            .stream()
            .filter(f -> f.getTemplatePath().equals(templatePath) && f.getFragmentName().equals(fragmentName))
            .findFirst();
    }
}
