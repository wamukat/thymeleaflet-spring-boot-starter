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
import java.util.Optional;

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
    public Optional<FragmentStoryInfo> getStory(String templatePath, String fragmentName, String storyName) {
        return storyDataPort.getStory(templatePath, fragmentName, storyName);
    }

    @Override
    public List<FragmentStoryInfo> getStoriesForFragment(FragmentDiscoveryService.FragmentInfo fragmentInfo) {
        // StoryDataPortを使用してストーリー設定を取得
        FragmentSummary domainFragmentSummary = fragmentSummaryMapper.toDomain(fragmentInfo);
        List<FragmentStoryInfo> stories = new ArrayList<>();

        Optional<StoryConfiguration> config = storyDataPort.loadStoryConfiguration(fragmentInfo.getTemplatePath());
        if (config.isPresent()) {
            StoryGroup group = config.orElseThrow().storyGroups().get(fragmentInfo.getFragmentName());
            if (group != null) {
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
            StoryItem defaultStory = new StoryItem(
                "default",
                "default",
                "",
                Collections.emptyMap(),
                StoryPreview.empty(),
                Collections.emptyMap()
            );
            stories.add(FragmentStoryInfo.of(
                domainFragmentSummary, 
                fragmentInfo.getFragmentName(),
                "default", 
                defaultStory
            ));
        }

        appendCustomStoryIfMissing(stories, domainFragmentSummary, fragmentInfo.getFragmentName());
        
        return stories;
    }

    private void appendCustomStoryIfMissing(List<FragmentStoryInfo> stories,
                                            FragmentSummary fragmentSummary,
                                            String fragmentGroupName) {
        if (!canUseCustomStory(stories, fragmentSummary)) {
            return;
        }
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

    private boolean canUseCustomStory(List<FragmentStoryInfo> stories, FragmentSummary fragmentSummary) {
        boolean hasParameters = !fragmentSummary.getParameters().isEmpty();
        if (hasParameters) {
            return true;
        }
        return stories.stream()
            .map(FragmentStoryInfo::getModel)
            .anyMatch(model -> !model.isEmpty());
    }

    @Override
    public StoryListResponse getStoriesForFragment(String templatePath, String fragmentName) {
        return fragmentDiscoveryService.discoverFragments().stream()
            .filter(f -> f.getTemplatePath().equals(templatePath) && f.getFragmentName().equals(fragmentName))
            .findFirst()
            .map(fragment -> StoryListResponse.success(fragment, getStoriesForFragment(fragment)))
            .orElseGet(StoryListResponse::failure);
    }

    @Autowired
    private FragmentDiscoveryService fragmentDiscoveryService;
    
    @Autowired
    private FragmentSummaryMapper fragmentSummaryMapper;
    
}
