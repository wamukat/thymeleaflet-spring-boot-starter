package io.github.wamukat.thymeleaflet.application.service.story;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.application.port.outbound.FragmentCatalogPort;
import io.github.wamukat.thymeleaflet.application.port.outbound.StoryDataPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryGroup;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryPreview;
import io.github.wamukat.thymeleaflet.domain.service.ParserDiagnostic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private final StoryDataPort storyDataPort;

    private final FragmentCatalogPort fragmentCatalogPort;

    public StoryRetrievalUseCaseImpl(StoryDataPort storyDataPort, FragmentCatalogPort fragmentCatalogPort) {
        this.storyDataPort = storyDataPort;
        this.fragmentCatalogPort = fragmentCatalogPort;
    }

    @Override
    public Optional<FragmentStoryInfo> getStory(String templatePath, String fragmentName, String storyName) {
        return storyDataPort.getStory(templatePath, fragmentName, storyName);
    }

    @Override
    public List<FragmentStoryInfo> getStoriesForFragment(FragmentSummary fragmentSummary) {
        // StoryDataPortを使用してストーリー設定を取得
        List<FragmentStoryInfo> stories = new ArrayList<>();

        Optional<StoryConfiguration> config = storyDataPort.loadStoryConfiguration(fragmentSummary.getTemplatePath());
        if (config.isPresent()) {
            Optional<StoryGroup> group = config.orElseThrow().getStoryGroup(fragmentSummary.getFragmentName());
            if (group.isPresent()) {
                for (StoryItem story : group.orElseThrow().stories()) {
                    stories.add(FragmentStoryInfo.of(
                        fragmentSummary,
                        fragmentSummary.getFragmentName(),
                        story.name(),
                        story
                    ));
                }
            }
        }
        
        // ストーリーが定義されていない場合はデフォルトストーリーを作成
        if (stories.isEmpty()) {
            // stories.yml が存在しない fallback default は hasStoryConfig=false として扱う
            stories.add(FragmentStoryInfo.fallback(
                fragmentSummary,
                fragmentSummary.getFragmentName(),
                "default"
            ));
        }

        appendCustomStoryIfMissing(stories, fragmentSummary, fragmentSummary.getFragmentName());
        
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
            Collections.emptyMap(),
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
        return true;
    }

    @Override
    public StoryListResponse getStoriesForFragment(String templatePath, String fragmentName) {
        return fragmentCatalogPort.findFragment(templatePath, fragmentName)
            .map(fragmentSummary -> StoryListResponse.success(fragmentSummary, getStoriesForFragment(fragmentSummary)))
            .orElseGet(StoryListResponse::failure);
    }

    @Override
    public Optional<StoryConfigurationDiagnostic> getStoryConfigurationDiagnostic(String templatePath) {
        Optional<StoryConfigurationDiagnostic> storyDiagnostic = storyDataPort.getStoryConfigurationDiagnostic(templatePath)
            .map(diagnostic -> new StoryConfigurationDiagnostic(
                diagnostic.code(),
                diagnostic.userSafeMessage(),
                diagnostic.developerMessage()
            ));
        if (storyDiagnostic.isPresent()) {
            return storyDiagnostic;
        }
        List<ParserDiagnostic> parserDiagnostics = fragmentCatalogPort.getTemplateParserDiagnostics(templatePath);
        if (parserDiagnostics.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toStoryConfigurationDiagnostic(parserDiagnostics));
    }

    private StoryConfigurationDiagnostic toStoryConfigurationDiagnostic(List<ParserDiagnostic> diagnostics) {
        if (diagnostics.size() == 1) {
            ParserDiagnostic diagnostic = diagnostics.getFirst();
            return new StoryConfigurationDiagnostic(
                diagnostic.code(),
                "Some template syntax was skipped while analyzing this story.",
                diagnosticDeveloperMessage(diagnostic),
                List.of(toDiagnosticItem(diagnostic))
            );
        }
        return new StoryConfigurationDiagnostic(
            "TEMPLATE_PARSER_DIAGNOSTICS",
            "Some template syntax was skipped while analyzing this story.",
            diagnostics.stream()
                .map(this::diagnosticDeveloperMessage)
                .collect(Collectors.joining("\n")),
            diagnostics.stream()
                .map(this::toDiagnosticItem)
                .toList()
        );
    }

    private StoryRetrievalUseCase.DiagnosticItem toDiagnosticItem(ParserDiagnostic diagnostic) {
        return new StoryRetrievalUseCase.DiagnosticItem(
            diagnostic.code(),
            diagnosticUserSafeMessage(diagnostic)
        );
    }

    private String diagnosticUserSafeMessage(ParserDiagnostic diagnostic) {
        if (diagnostic.line() > 0 && diagnostic.column() > 0) {
            return diagnostic.code() + " at line " + diagnostic.line() + ", column " + diagnostic.column();
        }
        return diagnostic.code();
    }

    private String diagnosticDeveloperMessage(ParserDiagnostic diagnostic) {
        String location = "";
        if (diagnostic.line() > 0 && diagnostic.column() > 0) {
            location = " at line " + diagnostic.line() + ", column " + diagnostic.column();
        }
        return diagnostic.message() + location;
    }

}
