package io.github.wamukat.thymeleaflet.application.service.story;

import io.github.wamukat.thymeleaflet.application.port.outbound.StoryDataPort;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase.DiagnosticItem;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase.StoryConfigurationDiagnostic;
import io.github.wamukat.thymeleaflet.application.port.outbound.FragmentCatalogPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryGroup;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryMeta;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryPreview;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.domain.service.ParserDiagnostic;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryRetrievalUseCaseImplTest {

    @Mock
    private StoryDataPort storyDataPort;

    @Mock
    private FragmentCatalogPort fragmentCatalogPort;

    @Mock
    private FragmentSummaryMapper fragmentSummaryMapper;

    @Mock
    private FragmentDiscoveryService fragmentDiscoveryService;

    @InjectMocks
    private StoryRetrievalUseCaseImpl useCase;

    @Test
    void shouldAppendCustomStoryWhenFragmentHasNoParametersAndNoModel() {
        FragmentSummary fragmentSummary = FragmentSummary.of(
            "components/icon",
            "simpleIcon",
            List.of(),
            FragmentDomainService.FragmentType.SIMPLE
        );

        when(storyDataPort.loadStoryConfiguration("components/icon")).thenReturn(Optional.empty());

        List<FragmentStoryInfo> stories = useCase.getStoriesForFragment(fragmentSummary);

        assertThat(stories).extracting(FragmentStoryInfo::getStoryName)
            .containsExactly("default", "custom");
        assertThat(stories.get(0).hasStoryConfig()).isFalse();
        assertThat(stories.get(1).hasStoryConfig()).isTrue();
    }

    @Test
    void shouldAppendCustomStoryWhenFragmentHasParameters() {
        FragmentSummary fragmentSummary = FragmentSummary.of(
            "components/input",
            "textInput",
            List.of("value"),
            FragmentDomainService.FragmentType.PARAMETERIZED
        );

        when(storyDataPort.loadStoryConfiguration("components/input")).thenReturn(Optional.empty());

        List<FragmentStoryInfo> stories = useCase.getStoriesForFragment(fragmentSummary);

        assertThat(stories).extracting(FragmentStoryInfo::getStoryName)
            .containsExactly("default", "custom");
        assertThat(stories.get(0).hasStoryConfig()).isFalse();
        assertThat(stories.get(1).hasStoryConfig()).isTrue();
    }

    @Test
    void shouldAppendCustomStoryWhenAnyStoryHasModel() {
        FragmentSummary fragmentSummary = FragmentSummary.of(
            "components/profile",
            "summary",
            List.of(),
            FragmentDomainService.FragmentType.DATA_DEPENDENT
        );
        StoryItem defaultStory = new StoryItem(
            "default",
            "default",
            "",
            Collections.emptyMap(),
            StoryPreview.empty(),
            Collections.emptyMap(),
            Map.of("profile", Map.of("name", "Sample"))
        );
        StoryGroup storyGroup = new StoryGroup("summary", "", List.of(defaultStory));
        StoryConfiguration configuration = new StoryConfiguration(
            new StoryMeta("meta", ""),
            Map.of("summary", storyGroup)
        );

        when(storyDataPort.loadStoryConfiguration("components/profile")).thenReturn(Optional.of(configuration));

        List<FragmentStoryInfo> stories = useCase.getStoriesForFragment(fragmentSummary);

        assertThat(stories).extracting(FragmentStoryInfo::getStoryName)
            .containsExactly("default", "custom");
    }

    @Test
    void shouldMapStoryConfigurationDiagnosticFromStoryDataPort() {
        StoryDataPort.StoryConfigurationDiagnostic outboundDiagnostic =
            new StoryDataPort.StoryConfigurationDiagnostic(
                "STORY_YAML_LOAD_FAILED",
                "Story configuration could not be loaded or parsed.",
                "Failed to parse classpath:META-INF/thymeleaflet/stories/components/profile.stories.yml"
        );
        when(storyDataPort.getStoryConfigurationDiagnostic("components/profile"))
            .thenReturn(Optional.of(outboundDiagnostic));

        Optional<StoryConfigurationDiagnostic> diagnostic = useCase.getStoryConfigurationDiagnostic("components/profile");

        assertThat(diagnostic).hasValueSatisfying(mappedDiagnostic -> {
            assertThat(mappedDiagnostic.code()).isEqualTo("STORY_YAML_LOAD_FAILED");
            assertThat(mappedDiagnostic.userSafeMessage())
                .isEqualTo("Story configuration could not be loaded or parsed.");
            assertThat(mappedDiagnostic.developerMessage())
                .isEqualTo("Failed to parse classpath:META-INF/thymeleaflet/stories/components/profile.stories.yml");
        });
    }

    @Test
    void shouldReturnEmptyStoryConfigurationDiagnosticWhenStoryDataPortHasNoDiagnostic() {
        when(storyDataPort.getStoryConfigurationDiagnostic("components/profile")).thenReturn(Optional.empty());
        when(fragmentCatalogPort.getTemplateParserDiagnostics("components/profile")).thenReturn(List.of());

        Optional<StoryConfigurationDiagnostic> diagnostic = useCase.getStoryConfigurationDiagnostic("components/profile");

        assertThat(diagnostic).isEmpty();
    }

    @Test
    void shouldMapParserDiagnosticWhenStoryConfigurationHasNoDiagnostic() {
        when(storyDataPort.getStoryConfigurationDiagnostic("components/profile")).thenReturn(Optional.empty());
        when(fragmentCatalogPort.getTemplateParserDiagnostics("components/profile"))
            .thenReturn(List.of(new ParserDiagnostic(
                "TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED",
                "Dynamic fragment reference was skipped in `th:replace`",
                3,
                12
            )));

        Optional<StoryConfigurationDiagnostic> diagnostic = useCase.getStoryConfigurationDiagnostic("components/profile");

        assertThat(diagnostic).hasValueSatisfying(mappedDiagnostic -> {
            assertThat(mappedDiagnostic.code()).isEqualTo("TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED");
            assertThat(mappedDiagnostic.userSafeMessage())
                .isEqualTo("Some template syntax was skipped while analyzing this story.");
            assertThat(mappedDiagnostic.developerMessage())
                .contains("Dynamic fragment reference was skipped in `th:replace`")
                .contains("line 3")
                .contains("column 12");
            assertThat(mappedDiagnostic.items())
                .extracting(DiagnosticItem::userSafeMessage)
                .containsExactly("TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED at line 3, column 12");
        });
    }

    @Test
    void shouldMapDuplicateFragmentParameterDiagnosticWhenStoryConfigurationHasNoDiagnostic() {
        when(storyDataPort.getStoryConfigurationDiagnostic("components/profile")).thenReturn(Optional.empty());
        when(fragmentCatalogPort.getTemplateParserDiagnostics("components/profile"))
            .thenReturn(List.of(new ParserDiagnostic(
                "FRAGMENT_SIGNATURE_DUPLICATE_PARAMETER",
                "Fragment `profileCard` declares duplicate parameter `name`.",
                2,
                14
            )));

        Optional<StoryConfigurationDiagnostic> diagnostic = useCase.getStoryConfigurationDiagnostic("components/profile");

        assertThat(diagnostic).hasValueSatisfying(mappedDiagnostic -> {
            assertThat(mappedDiagnostic.code()).isEqualTo("FRAGMENT_SIGNATURE_DUPLICATE_PARAMETER");
            assertThat(mappedDiagnostic.developerMessage())
                .contains("duplicate parameter `name`")
                .contains("line 2")
                .contains("column 14");
            assertThat(mappedDiagnostic.items())
                .extracting(DiagnosticItem::userSafeMessage)
                .containsExactly("FRAGMENT_SIGNATURE_DUPLICATE_PARAMETER at line 2, column 14");
        });
    }

    @Test
    void shouldMapMultipleParserDiagnosticsWhenStoryConfigurationHasNoDiagnostic() {
        when(storyDataPort.getStoryConfigurationDiagnostic("components/profile")).thenReturn(Optional.empty());
        when(fragmentCatalogPort.getTemplateParserDiagnostics("components/profile"))
            .thenReturn(List.of(
                new ParserDiagnostic(
                    "TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED",
                    "Dynamic fragment reference was skipped in `th:replace`",
                    3,
                    12
                ),
                ParserDiagnostic.warning(
                    "FRAGMENT_EXPRESSION_MALFORMED",
                    "Fragment expression is missing a valid template/fragment separator: ~{components/card}"
                )
            ));

        Optional<StoryConfigurationDiagnostic> diagnostic = useCase.getStoryConfigurationDiagnostic("components/profile");

        assertThat(diagnostic).hasValueSatisfying(mappedDiagnostic -> {
            assertThat(mappedDiagnostic.code()).isEqualTo("TEMPLATE_PARSER_DIAGNOSTICS");
            assertThat(mappedDiagnostic.userSafeMessage())
                .isEqualTo("Some template syntax was skipped while analyzing this story.");
            assertThat(mappedDiagnostic.developerMessage())
                .contains("Dynamic fragment reference was skipped in `th:replace`")
                .contains("Fragment expression is missing a valid template/fragment separator");
            assertThat(mappedDiagnostic.items())
                .extracting(DiagnosticItem::userSafeMessage)
                .containsExactly(
                    "TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED at line 3, column 12",
                    "FRAGMENT_EXPRESSION_MALFORMED"
                );
        });
    }
}
