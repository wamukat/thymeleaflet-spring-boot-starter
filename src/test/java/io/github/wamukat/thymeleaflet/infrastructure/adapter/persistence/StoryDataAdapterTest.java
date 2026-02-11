package io.github.wamukat.thymeleaflet.infrastructure.adapter.persistence;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryGroup;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryMeta;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryDataAdapterTest {

    @Mock
    private YamlStoryConfigurationLoader yamlStoryConfigurationLoader;

    @Mock
    private FragmentDiscoveryService fragmentDiscoveryService;

    @Mock
    private FragmentSummaryMapper fragmentSummaryMapper;

    private StoryDataAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StoryDataAdapter(yamlStoryConfigurationLoader);
        ReflectionTestUtils.setField(adapter, "fragmentDiscoveryService", fragmentDiscoveryService);
        ReflectionTestUtils.setField(adapter, "fragmentSummaryMapper", fragmentSummaryMapper);
    }

    @Test
    void getStory_shouldMarkFallbackDefaultAsNoStoryConfig_whenStoryFileMissing() {
        String templatePath = "components/ui-form";
        String fragmentName = "selectField";

        FragmentDiscoveryService.FragmentInfo fragmentInfo = new FragmentDiscoveryService.FragmentInfo(
            templatePath,
            fragmentName,
            List.of("options"),
            FragmentDomainService.FragmentType.PARAMETERIZED,
            "th:fragment=\"selectField(options)\""
        );
        FragmentSummary summary = FragmentSummary.parameterized(templatePath, fragmentName, List.of("options"));

        when(yamlStoryConfigurationLoader.loadStoryConfiguration(templatePath)).thenReturn(Optional.empty());
        when(fragmentDiscoveryService.discoverFragments()).thenReturn(List.of(fragmentInfo));
        when(fragmentSummaryMapper.toDomain(fragmentInfo)).thenReturn(summary);

        Optional<FragmentStoryInfo> story = adapter.getStory(templatePath, fragmentName, "default");

        assertThat(story).isPresent();
        assertThat(story.orElseThrow().getStoryName()).isEqualTo("default");
        assertThat(story.orElseThrow().hasStoryConfig()).isFalse();
    }

    @Test
    void getStory_shouldMarkFallbackDefaultAsNoStoryConfig_whenRequestedStoryMissingInFile() {
        String templatePath = "components/ui-form";
        String fragmentName = "selectField";

        FragmentDiscoveryService.FragmentInfo fragmentInfo = new FragmentDiscoveryService.FragmentInfo(
            templatePath,
            fragmentName,
            List.of("options"),
            FragmentDomainService.FragmentType.PARAMETERIZED,
            "th:fragment=\"selectField(options)\""
        );
        FragmentSummary summary = FragmentSummary.parameterized(templatePath, fragmentName, List.of("options"));

        StoryGroup storyGroup = new StoryGroup("Select field", "", List.of());
        StoryConfiguration config = new StoryConfiguration(new StoryMeta("meta", ""), Map.of(fragmentName, storyGroup));

        when(yamlStoryConfigurationLoader.loadStoryConfiguration(templatePath)).thenReturn(Optional.of(config));
        when(fragmentDiscoveryService.discoverFragments()).thenReturn(List.of(fragmentInfo));
        when(fragmentSummaryMapper.toDomain(fragmentInfo)).thenReturn(summary);

        Optional<FragmentStoryInfo> story = adapter.getStory(templatePath, fragmentName, "unknown");

        assertThat(story).isPresent();
        assertThat(story.orElseThrow().getStoryName()).isEqualTo("unknown");
        assertThat(story.orElseThrow().hasStoryConfig()).isFalse();
    }
}
