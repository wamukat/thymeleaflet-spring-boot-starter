package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.ValidationUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.web.rendering.ThymeleafFragmentRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FragmentRenderingServiceTest {

    private final ValidationUseCase validationUseCase = mock(ValidationUseCase.class);
    private final StoryRetrievalUseCase storyRetrievalUseCase = mock(StoryRetrievalUseCase.class);
    private final StoryParameterUseCase storyParameterUseCase = mock(StoryParameterUseCase.class);
    private final SecurePathConversionService securePathConversionService = mock(SecurePathConversionService.class);
    private final ThymeleafFragmentRenderer thymeleafFragmentRenderer = mock(ThymeleafFragmentRenderer.class);
    private final JavaDocLookupService javaDocLookupService = mock(JavaDocLookupService.class);
    private final FragmentModelInferenceService fragmentModelInferenceService = mock(FragmentModelInferenceService.class);
    private final StoryJavaTimeValueCoercionService storyJavaTimeValueCoercionService =
        new StoryJavaTimeValueCoercionService();
    private final FragmentRenderingService service = new FragmentRenderingService(
        validationUseCase,
        storyRetrievalUseCase,
        storyParameterUseCase,
        securePathConversionService,
        thymeleafFragmentRenderer,
        new StaticMessageSource(),
        new DefaultResourceLoader(),
        fragmentModelInferenceService,
        javaDocLookupService,
        storyJavaTimeValueCoercionService
    );

    @Test
    void shouldSkipValueAssemblyForSimpleFragment() {
        ExtendedModelMap model = new ExtendedModelMap();
        FragmentStoryInfo storyInfo = FragmentStoryInfo.fallback(
            FragmentSummary.simple("components/badge", "statusBadge"),
            "components.badge",
            "default"
        );
        when(securePathConversionService.convertSecurePath("components.badge.statusBadge", model))
            .thenReturn(SecurePathConversionService.SecurityConversionResult.success("components/badge"));
        when(storyRetrievalUseCase.getStory("components/badge", "statusBadge", "default"))
            .thenReturn(Optional.of(storyInfo));
        when(javaDocLookupService.findJavaDocInfo("components/badge", "statusBadge")).thenReturn(Optional.empty());
        when(fragmentModelInferenceService.inferModel("components/badge", "statusBadge", List.of()))
            .thenReturn(Map.of());
        when(fragmentModelInferenceService.inferMethodReturnCandidates("components/badge", "statusBadge", List.of()))
            .thenReturn(Map.of());

        FragmentRenderingService.RenderingResult result = service.renderStory(
            "components.badge.statusBadge",
            "statusBadge",
            "default",
            model
        );

        assertThat(result.succeeded()).isTrue();
        assertThat(result.templateReference()).contains("components/badge :: statusBadge");
        verify(storyParameterUseCase, never()).getParametersForStory(storyInfo);
    }

    @Test
    void shouldSkipValueAssemblyForNoParameterDataDependentFragment() {
        ExtendedModelMap model = new ExtendedModelMap();
        FragmentStoryInfo storyInfo = FragmentStoryInfo.fallback(
            FragmentSummary.of(
                "components/panel",
                "emptyPanel",
                List.of(),
                FragmentDomainService.FragmentType.DATA_DEPENDENT
            ),
            "components.panel",
            "default"
        );
        when(securePathConversionService.convertSecurePath("components.panel.emptyPanel", model))
            .thenReturn(SecurePathConversionService.SecurityConversionResult.success("components/panel"));
        when(storyRetrievalUseCase.getStory("components/panel", "emptyPanel", "default"))
            .thenReturn(Optional.of(storyInfo));
        when(javaDocLookupService.findJavaDocInfo("components/panel", "emptyPanel")).thenReturn(Optional.empty());
        when(fragmentModelInferenceService.inferModel("components/panel", "emptyPanel", List.of()))
            .thenReturn(Map.of("view", Map.of("title", "Empty panel")));
        when(fragmentModelInferenceService.inferMethodReturnCandidates("components/panel", "emptyPanel", List.of()))
            .thenReturn(Map.of());
        when(thymeleafFragmentRenderer.resolveTemplateValue(Map.of("title", "Empty panel")))
            .thenReturn(Map.of("title", "Empty panel"));

        FragmentRenderingService.RenderingResult result = service.renderStory(
            "components.panel.emptyPanel",
            "emptyPanel",
            "default",
            model,
            Map.of(),
            Map.of(),
            Map.of()
        );

        assertThat(result.succeeded()).isTrue();
        assertThat(result.templateReference()).contains("components/panel :: emptyPanel");
        assertThat(model.getAttribute("view")).isEqualTo(Map.of("title", "Empty panel"));
        verify(storyParameterUseCase, never()).getParametersForStory(storyInfo);
    }
}
