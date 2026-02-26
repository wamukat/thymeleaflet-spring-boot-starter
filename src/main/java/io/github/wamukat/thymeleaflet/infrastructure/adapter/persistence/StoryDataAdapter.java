package io.github.wamukat.thymeleaflet.infrastructure.adapter.persistence;

import io.github.wamukat.thymeleaflet.application.port.outbound.StoryDataPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryGroup;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            Optional<StoryConfiguration> config = loadStoryConfiguration(
                storyInfo.getFragmentSummary().getTemplatePath()
            );

            if (config.isEmpty()) {
                return parameters;
            }

            String fragmentName = storyInfo.getFragmentSummary().getFragmentName();
            String storyName = storyInfo.getStoryName();

            // フラグメント名に対応するStoryGroupを取得
            Optional<StoryGroup> fragmentGroup = config.orElseThrow().getStoryGroup(fragmentName);
            if (fragmentGroup.isEmpty()) {
                return parameters;
            }

            // 指定されたストーリー名に対応するストーリーを検索
            fragmentGroup.orElseThrow().stories().stream()
                .filter(story -> storyName.equals(story.name()))
                .findFirst()
                .ifPresent(story -> {
                    parameters.putAll(story.parameters());
                    logger.debug("Loaded story parameters for {}::{}: {}",
                        fragmentName, storyName, parameters);
                });
            
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
            Optional<StoryGroup> storyGroupOptional = storyConfiguration.get().getStoryGroup(fragmentName);
            if (storyGroupOptional.isEmpty()) {
                logger.debug("Story group not found for fragment: {}, returning default story", fragmentName);
                return createDefaultStory(templatePath, fragmentName, storyName);
            }
            StoryGroup storyGroup = storyGroupOptional.orElseThrow();
            
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

                StoryItem customStory = createCustomStoryFromBase(baseStory.get());

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

        String resolvedStoryName = storyName.isBlank() ? "default" : storyName;
        
        io.github.wamukat.thymeleaflet.domain.model.FragmentSummary domainFragmentSummary = 
            fragmentSummaryMapper.toDomain(fragmentInfo.get());
        return Optional.of(FragmentStoryInfo.fallback(
            domainFragmentSummary,
            fragmentName,
            resolvedStoryName
        ));
    }

    private Optional<StoryItem> resolveBaseStory(StoryGroup storyGroup) {
        if (storyGroup.stories().isEmpty()) {
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

    private StoryItem createCustomStoryFromBase(StoryItem baseStory) {
        Map<String, Object> normalizedModel = removeMethodReturnPathsFromModel(
            baseStory.model(),
            baseStory.methodReturns()
        );
        return new StoryItem(
            "custom",
            "Custom",
            "",
            baseStory.parameters(),
            baseStory.preview(),
            normalizedModel,
            baseStory.methodReturns()
        );
    }

    private Map<String, Object> removeMethodReturnPathsFromModel(
        Map<String, Object> model,
        Map<String, Object> methodReturns
    ) {
        Map<String, Object> normalizedModel = toStringKeyMap(model);
        if (normalizedModel.isEmpty() || methodReturns.isEmpty()) {
            return normalizedModel;
        }

        List<List<String>> methodReturnPaths = new ArrayList<>();
        collectLeafPaths(methodReturns, new ArrayList<>(), methodReturnPaths);
        for (List<String> path : methodReturnPaths) {
            removePath(normalizedModel, path, 0);
        }
        return normalizedModel;
    }

    private void collectLeafPaths(
        Map<?, ?> source,
        List<String> parentSegments,
        List<List<String>> collectedPaths
    ) {
        source.forEach((rawKey, rawValue) -> {
            String key = String.valueOf(rawKey);
            List<String> nextSegments = new ArrayList<>(parentSegments);
            nextSegments.add(key);
            if (rawValue instanceof Map<?, ?> nestedMap && !nestedMap.isEmpty()) {
                collectLeafPaths(nestedMap, nextSegments, collectedPaths);
                return;
            }
            collectedPaths.add(nextSegments);
        });
    }

    private boolean removePath(Map<String, Object> target, List<String> segments, int index) {
        if (segments.isEmpty() || index >= segments.size()) {
            return target.isEmpty();
        }
        String current = segments.get(index);
        if (!target.containsKey(current)) {
            return target.isEmpty();
        }
        if (index == segments.size() - 1) {
            target.remove(current);
            return target.isEmpty();
        }

        Object child = target.get(current);
        if (!(child instanceof Map<?, ?> childMap)) {
            return target.isEmpty();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> castedChild = (Map<String, Object>) childMap;
        boolean childEmpty = removePath(castedChild, segments, index + 1);
        if (childEmpty) {
            target.remove(current);
        }
        return target.isEmpty();
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> rawMap) {
        Map<String, Object> converted = new HashMap<>();
        rawMap.forEach((key, value) -> converted.put(String.valueOf(key), deepCopyValue(value)));
        return converted;
    }

    private Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            return toStringKeyMap(mapValue);
        }
        if (value instanceof List<?> listValue) {
            List<Object> copied = new ArrayList<>(listValue.size());
            listValue.forEach(item -> copied.add(deepCopyValue(item)));
            return copied;
        }
        return value;
    }
}
