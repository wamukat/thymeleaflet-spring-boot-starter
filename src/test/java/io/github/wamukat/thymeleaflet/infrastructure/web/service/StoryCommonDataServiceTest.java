package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryPreview;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import io.github.wamukat.thymeleaflet.infrastructure.web.rendering.ThymeleafFragmentRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryCommonDataServiceTest {

    @Mock
    private StoryParameterUseCase storyParameterUseCase;

    @Mock
    private ThymeleafFragmentRenderer thymeleafFragmentRenderer;

    @Mock
    private StoryRetrievalUseCase storyRetrievalUseCase;

    @Mock
    private JavaDocLookupService javaDocLookupService;

    @Mock
    private FragmentDependencyService fragmentDependencyService;

    @Test
    void setupCommonStoryData_setsPreviewResourcesFromProperties() {
        StoryCommonDataService service = new StoryCommonDataService();

        StorybookProperties properties = new StorybookProperties();
        StorybookProperties.ResourceConfig resources = new StorybookProperties.ResourceConfig();
        resources.setStylesheets(List.of(" /css/app.css ", "/css/theme.css"));
        resources.setScripts(List.of("/js/app.js", " /js/vendor.js "));
        properties.setResources(resources);
        PreviewConfigService previewConfigService = buildPreviewConfigService(properties);

        ReflectionTestUtils.setField(service, "storybookProperties", properties);
        ReflectionTestUtils.setField(service, "storyParameterUseCase", storyParameterUseCase);
        ReflectionTestUtils.setField(service, "thymeleafFragmentRenderer", thymeleafFragmentRenderer);
        ReflectionTestUtils.setField(service, "storyRetrievalUseCase", storyRetrievalUseCase);
        ReflectionTestUtils.setField(service, "javaDocLookupService", javaDocLookupService);
        ReflectionTestUtils.setField(service, "fragmentDependencyService", fragmentDependencyService);
        ReflectionTestUtils.setField(service, "previewConfigService", previewConfigService);

        FragmentSummary summary = FragmentSummary.of(
            "components/button",
            "primaryButton",
            List.of("label"),
            FragmentDomainService.FragmentType.PARAMETERIZED
        );
        StoryItem story = new StoryItem(
            "default",
            "Default",
            "",
            Map.of(),
            StoryPreview.empty(),
            Map.of()
        );
        FragmentStoryInfo storyInfo = FragmentStoryInfo.of(summary, "components", "default", story);

        when(storyParameterUseCase.getParametersForStory(storyInfo)).thenReturn(Map.of());
        when(thymeleafFragmentRenderer.configureModelWithStoryParameters(any(), any()))
            .thenReturn(Map.of());
        when(fragmentDependencyService.findDependencies("components/button", "primaryButton")).thenReturn(List.of());

        Model model = new ExtendedModelMap();

        service.setupCommonStoryData("components/button", "primaryButton", "default", storyInfo, model);

        assertThat(model.getAttribute("previewStylesheets")).isEqualTo("/css/app.css,/css/theme.css");
        assertThat(model.getAttribute("previewScripts")).isEqualTo("/js/app.js,/js/vendor.js");
    }

    private PreviewConfigService buildPreviewConfigService(StorybookProperties properties) {
        PreviewConfigService previewConfigService = new PreviewConfigService();
        StaticMessageSource messageSource = new StaticMessageSource();
        ReflectionTestUtils.setField(previewConfigService, "storybookProperties", properties);
        ReflectionTestUtils.setField(previewConfigService, "messageSource", messageSource);
        return previewConfigService;
    }
}
