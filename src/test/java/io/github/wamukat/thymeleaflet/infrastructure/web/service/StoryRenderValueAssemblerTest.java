package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryPreview;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import io.github.wamukat.thymeleaflet.infrastructure.web.rendering.PreviewWarningRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StoryRenderValueAssemblerTest {

    private final StoryParameterUseCase storyParameterUseCase = mock(StoryParameterUseCase.class);
    private final FragmentModelInferenceService fragmentModelInferenceService = mock(FragmentModelInferenceService.class);
    private final StoryJavaTimeValueCoercionService storyJavaTimeValueCoercionService =
        new StoryJavaTimeValueCoercionService();
    private final StaticMessageSource messageSource = new StaticMessageSource();
    private final StoryRenderValueAssembler assembler = new StoryRenderValueAssembler(
        storyParameterUseCase,
        fragmentModelInferenceService,
        storyJavaTimeValueCoercionService,
        messageSource
    );

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldAssembleFallbackValuesWithExpectedOverridePrecedence() {
        FragmentStoryInfo storyInfo = FragmentStoryInfo.fallback(
            FragmentSummary.parameterized("components/select", "selectInput", List.of("label")),
            "components.select",
            "default"
        );
        JavaDocAnalyzer.JavaDocInfo javaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "Select",
            List.of(JavaDocAnalyzer.ParameterInfo.optional("label", "String", "JavaDoc label", "label")),
            List.of(
                JavaDocAnalyzer.ModelInfo.optional("view.title", "String", "JavaDoc title", "title"),
                JavaDocAnalyzer.ModelInfo.optional("view.enabled", "Boolean", "true", "enabled")
            ),
            List.of(),
            Optional.empty()
        );

        when(storyParameterUseCase.getParametersForStory(storyInfo)).thenReturn(Map.of("label", "fallback label"));
        when(fragmentModelInferenceService.inferModel("components/select", "selectInput", List.of("label")))
            .thenReturn(Map.of("view", Map.of("title", "Inferred title", "count", 0)));
        when(fragmentModelInferenceService.inferMethodReturnCandidates("components/select", "selectInput", List.of("label")))
            .thenReturn(Map.of("view", Map.of("hasOptions", false)));

        StoryRenderValueAssembler.StoryRenderValues values = assembler.assemble(
            new StoryRenderValueAssembler.RenderValueAssemblyRequest(
                storyInfo,
                Optional.of(javaDocInfo),
                "components/select",
                "selectInput",
                Map.of("label", "Override label"),
                Map.of("view", Map.of("count", 99)),
                Map.of()
            )
        );

        assertThat(values.parameters()).containsEntry("label", "Override label");
        assertThat(values.model()).containsEntry(
            "view",
            Map.of("title", "JavaDoc title", "count", 99, "enabled", true, "hasOptions", false)
        );
        assertThat(values.methodReturns()).containsEntry("view", Map.of("hasOptions", false));
    }

    @Test
    void shouldKeepConfiguredModelOverMethodReturnAndRecordConflictWarning() {
        messageSource.addMessage(
            "thymeleaflet.preview.warning.methodReturnConflict",
            Locale.getDefault(),
            "Conflict at {0}"
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        PreviewWarningRecorder.clear();

        FragmentStoryInfo storyInfo = FragmentStoryInfo.of(
            FragmentSummary.parameterized("components/card", "summaryCard", List.of("label")),
            "components.card",
            "default",
            new StoryItem(
                "default",
                "Default",
                "",
                Map.of("label", "Story label"),
                StoryPreview.empty(),
                Map.of("view", Map.of("title", "Story title")),
                Map.of("view", Map.of("title", "Method title", "enabled", false))
            )
        );
        when(storyParameterUseCase.getParametersForStory(storyInfo)).thenReturn(Map.of("label", "Story label"));

        StoryRenderValueAssembler.StoryRenderValues values = assembler.assemble(
            new StoryRenderValueAssembler.RenderValueAssemblyRequest(
                storyInfo,
                Optional.empty(),
                "components/card",
                "summaryCard",
                Map.of(),
                Map.of(),
                Map.of()
            )
        );

        assertThat(values.model()).containsEntry(
            "view",
            Map.of("title", "Story title", "enabled", false)
        );
        assertThat(values.parameters()).containsEntry("label", "Story label");

        String warningsHeader = response.getHeader(PreviewWarningRecorder.HEADER_NAME);
        assertThat(warningsHeader).isNotBlank();
        String decoded = new String(Base64.getUrlDecoder().decode(warningsHeader), StandardCharsets.UTF_8);
        assertThat(decoded).contains("Conflict at view.title");
    }
}
