package io.github.wamukat.thymeleaflet.application.service.story;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.application.port.outbound.StoryDataPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryGroup;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryPreview;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ストーリー取得専用ユースケース実装
 * 
 * 責務: ストーリー取得のみ
 * SRP準拠: 単一責任原則に従い、ストーリー取得のみを担当
 */
@Component
@Transactional(readOnly = true)
public class StoryRetrievalUseCaseImpl implements StoryRetrievalUseCase {

    private static final String CUSTOM_STORY_NAME = "custom";
    
    @Autowired
    private StoryDataPort storyDataPort;

    @Override
    public FragmentStoryInfo getStory(String templatePath, String fragmentName, String storyName) {
        return storyDataPort.getStory(templatePath, fragmentName, storyName);
    }

    @Override
    public List<FragmentStoryInfo> getStoriesForFragment(FragmentDiscoveryService.FragmentInfo fragmentInfo) {
        // StoryDataPortを使用してストーリー設定を取得
        StoryConfiguration config = storyDataPort.loadStoryConfiguration(fragmentInfo.getTemplatePath());
        
        FragmentSummary domainFragmentSummary = fragmentSummaryMapper.toDomain(fragmentInfo);
        List<FragmentStoryInfo> stories = new ArrayList<>();
        
        if (config != null && config.storyGroups() != null) {
            StoryGroup group = config.storyGroups().get(fragmentInfo.getFragmentName());
            
            if (group != null && group.stories() != null) {
                for (StoryItem story : group.stories()) {
                    stories.add(FragmentStoryInfo.of(
                        domainFragmentSummary, 
                        fragmentInfo.getFragmentName(),
                        story.name(), 
                        story
                    ));
                }
            }
        }
        
        // ストーリーが定義されていない場合はデフォルトストーリーを作成
        if (stories.isEmpty()) {
            stories.add(FragmentStoryInfo.of(
                domainFragmentSummary, 
                fragmentInfo.getFragmentName(),
                "default", 
                null
            ));
        }

        appendCustomStoryIfMissing(stories, domainFragmentSummary, fragmentInfo.getFragmentName());
        
        return stories;
    }

    private void appendCustomStoryIfMissing(List<FragmentStoryInfo> stories,
                                            FragmentSummary fragmentSummary,
                                            String fragmentGroupName) {
        boolean hasCustom = stories.stream()
            .anyMatch(story -> CUSTOM_STORY_NAME.equals(story.getStoryName()));
        if (hasCustom) {
            return;
        }
        StoryItem customStory = new StoryItem(
            CUSTOM_STORY_NAME,
            "Custom",
            "",
            Collections.emptyMap(),
            StoryPreview.empty(),
            Collections.emptyMap()
        );
        stories.add(FragmentStoryInfo.of(
            fragmentSummary,
            fragmentGroupName,
            CUSTOM_STORY_NAME,
            customStory
        ));
    }

    @Override
    public StoryListResponse getStoriesForFragment(String templatePath, String fragmentName) {
        // フラグメント情報を取得
        List<FragmentDiscoveryService.FragmentInfo> allFragments = fragmentDiscoveryService.discoverFragments();
        FragmentDiscoveryService.FragmentInfo fragment = allFragments.stream()
            .filter(f -> f.getTemplatePath().equals(templatePath) && f.getFragmentName().equals(fragmentName))
            .findFirst()
            .orElse(null);
        
        if (fragment == null) {
            return StoryListResponse.failure();
        }
        
        List<FragmentStoryInfo> stories = getStoriesForFragment(fragment);
        return StoryListResponse.success(fragment, stories);
    }

    @Autowired
    private FragmentDiscoveryService fragmentDiscoveryService;
    
    @Autowired
    private FragmentSummaryMapper fragmentSummaryMapper;
    
}
