package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StoryDiagnosticTemplateRenderingTest {

    private static final String DIAGNOSTIC_CODE = "STORY_YAML_LOAD_FAILED";
    private static final String USER_SAFE_MESSAGE = "Story YAML was found but could not be loaded.";
    private static final String DEVELOPER_MESSAGE = "SnakeYAML parser stack trace should stay server-side";

    private SpringTemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");

        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
    }

    @Test
    void shouldRenderStoryDiagnosticAlertWithoutDeveloperMessage() {
        WebContext context = baseContext();

        String html = templateEngine.process("thymeleaflet/test/story-diagnostic-host", context);

        assertThat(html)
            .contains(DIAGNOSTIC_CODE)
            .contains(USER_SAFE_MESSAGE)
            .doesNotContain(DEVELOPER_MESSAGE);
    }

    @Test
    void shouldRenderMultipleParserDiagnosticItemsWithoutDeveloperMessage() {
        WebContext context = baseContext();
        context.setVariable("storyConfigurationDiagnostic", new StoryRetrievalUseCase.StoryConfigurationDiagnostic(
            "TEMPLATE_PARSER_DIAGNOSTICS",
            "Some template syntax was skipped while analyzing this story.",
            DEVELOPER_MESSAGE,
            List.of(
                new StoryRetrievalUseCase.DiagnosticItem(
                    "TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED",
                    "TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED at line 3, column 12"
                ),
                new StoryRetrievalUseCase.DiagnosticItem(
                    "FRAGMENT_EXPRESSION_MALFORMED",
                    "FRAGMENT_EXPRESSION_MALFORMED"
                )
            )
        ));

        String html = templateEngine.process("thymeleaflet/test/story-diagnostic-host", context);

        assertThat(html)
            .contains("TEMPLATE_PARSER_DIAGNOSTICS")
            .contains("TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED at line 3, column 12")
            .contains("FRAGMENT_EXPRESSION_MALFORMED")
            .doesNotContain(DEVELOPER_MESSAGE);
    }

    @Test
    void shouldUseStoryDiagnosticAlertFromMainContentAndStoryPreviewTemplates() {
        assertThat(resourceText("templates/thymeleaflet/fragments/main-content.html"))
            .contains("story-configuration-diagnostic :: alert(${storyConfigurationDiagnostic})")
            .doesNotContain("storyConfigurationDiagnostic.developerMessage");
        assertThat(resourceText("templates/thymeleaflet/story-preview.html"))
            .contains("story-configuration-diagnostic :: alert(${storyConfigurationDiagnostic})")
            .doesNotContain("storyConfigurationDiagnostic.developerMessage");
    }

    private WebContext baseContext() {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(
            JakartaServletWebApplication.buildApplication(servletContext).buildExchange(request, response),
            Locale.ENGLISH
        );
        context.setVariable("storyConfigurationDiagnostic", diagnostic());
        context.setVariable("templatePathEncoded", "components.card");
        context.setVariable("fragmentName", "card");
        context.setVariable("storyName", "default");
        context.setVariable("displayParameters", Map.of());
        context.setVariable("displayModel", Map.of());
        context.setVariable("displayMethodReturns", Map.of());
        context.setVariable("orderedParameterNames", List.of());
        context.setVariable("dependentComponents", List.of());
        context.setVariable("previewViewportPresets", List.of());
        context.setVariable("basePath", "/thymeleaflet");
        return context;
    }

    private String resourceText(String path) {
        try (var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private StoryRetrievalUseCase.StoryConfigurationDiagnostic diagnostic() {
        return new StoryRetrievalUseCase.StoryConfigurationDiagnostic(
            DIAGNOSTIC_CODE,
            USER_SAFE_MESSAGE,
            DEVELOPER_MESSAGE
        );
    }

}
