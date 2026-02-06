package io.github.wamukat.thymeleaflet.application.service.story;

import io.github.wamukat.thymeleaflet.application.port.outbound.StoryDataPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryGroup;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryMeta;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryPreview;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryRetrievalUseCaseImplTest {

    @Mock
    private StoryDataPort storyDataPort;

    @Mock
    private FragmentSummaryMapper fragmentSummaryMapper;

    @Mock
    private FragmentDiscoveryService fragmentDiscoveryService;

    @InjectMocks
    private StoryRetrievalUseCaseImpl useCase;

    @Test
    void shouldNotAppendCustomStoryWhenFragmentHasNoParametersAndNoModel() {
        FragmentDiscoveryService.FragmentInfo fragmentInfo = new FragmentDiscoveryService.FragmentInfo(
            "components/icon",
            "simpleIcon",
            List.of(),
            FragmentDomainService.FragmentType.SIMPLE,
            "simpleIcon"
        );
        FragmentSummary fragmentSummary = FragmentSummary.of(
            "components/icon",
            "simpleIcon",
            List.of(),
            FragmentDomainService.FragmentType.SIMPLE
        );

        when(storyDataPort.loadStoryConfiguration("components/icon")).thenReturn(null);
        when(fragmentSummaryMapper.toDomain(fragmentInfo)).thenReturn(fragmentSummary);

        List<FragmentStoryInfo> stories = useCase.getStoriesForFragment(fragmentInfo);

        assertThat(stories).extracting(FragmentStoryInfo::getStoryName)
            .containsExactly("default");
    }

    @Test
    void shouldAppendCustomStoryWhenFragmentHasParameters() {
        FragmentDiscoveryService.FragmentInfo fragmentInfo = new FragmentDiscoveryService.FragmentInfo(
            "components/input",
            "textInput",
            List.of("value"),
            FragmentDomainService.FragmentType.PARAMETERIZED,
            "textInput(value)"
        );
        FragmentSummary fragmentSummary = FragmentSummary.of(
            "components/input",
            "textInput",
            List.of("value"),
            FragmentDomainService.FragmentType.PARAMETERIZED
        );

        when(storyDataPort.loadStoryConfiguration("components/input")).thenReturn(null);
        when(fragmentSummaryMapper.toDomain(fragmentInfo)).thenReturn(fragmentSummary);

        List<FragmentStoryInfo> stories = useCase.getStoriesForFragment(fragmentInfo);

        assertThat(stories).extracting(FragmentStoryInfo::getStoryName)
            .containsExactly("default", "custom");
    }

    @Test
    void shouldAppendCustomStoryWhenAnyStoryHasModel() {
        FragmentDiscoveryService.FragmentInfo fragmentInfo = new FragmentDiscoveryService.FragmentInfo(
            "components/profile",
            "summary",
            List.of(),
            FragmentDomainService.FragmentType.DATA_DEPENDENT,
            "summary"
        );
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
            Map.of("profile", Map.of("name", "Sample"))
        );
        StoryGroup storyGroup = new StoryGroup("summary", "", List.of(defaultStory));
        StoryConfiguration configuration = new StoryConfiguration(
            new StoryMeta("meta", ""),
            Map.of("summary", storyGroup)
        );

        when(storyDataPort.loadStoryConfiguration("components/profile")).thenReturn(configuration);
        when(fragmentSummaryMapper.toDomain(fragmentInfo)).thenReturn(fragmentSummary);

        List<FragmentStoryInfo> stories = useCase.getStoriesForFragment(fragmentInfo);

        assertThat(stories).extracting(FragmentStoryInfo::getStoryName)
            .containsExactly("default", "custom");
    }
}
