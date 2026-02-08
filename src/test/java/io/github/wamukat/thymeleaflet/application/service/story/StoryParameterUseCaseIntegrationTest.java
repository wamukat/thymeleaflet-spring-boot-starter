package io.github.wamukat.thymeleaflet.application.service.story;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryPreview;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {io.github.wamukat.thymeleaflet.config.FragmentTestConfiguration.class})
@ActiveProfiles("test")
class StoryParameterUseCaseIntegrationTest {

    @Autowired
    private StoryParameterUseCase storyParameterUseCase;

    @Test
    void getParametersForStory_shouldReturnStoryParameters_whenStoryDefinesParameters() {
        FragmentSummary fragmentSummary = FragmentSummary.of(
            "components/form-select",
            "selectInput",
            List.of("value"),
            FragmentDomainService.FragmentType.PARAMETERIZED
        );

        StoryItem storyItem = new StoryItem(
            "default",
            "Default",
            "",
            Map.of("value", "pro"),
            StoryPreview.empty(),
            Map.of()
        );

        FragmentStoryInfo storyInfo = FragmentStoryInfo.of(
            fragmentSummary,
            "selectInput",
            "default",
            storyItem
        );

        Map<String, Object> parameters = storyParameterUseCase.getParametersForStory(storyInfo);

        assertThat(parameters).containsEntry("value", "pro");
    }
}
