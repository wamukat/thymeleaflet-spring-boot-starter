package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.domain.service.FragmentExpressionParser;
import io.github.wamukat.thymeleaflet.domain.service.ParserDiagnostic;
import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;
import io.github.wamukat.thymeleaflet.infrastructure.cache.ThymeleafletCacheManager;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class FragmentDiscoveryServiceTemplateDiagnosticsTest {

    private final TemplateScanner templateScanner = mock(TemplateScanner.class);
    private final FragmentDiscoveryService discoveryService = new FragmentDiscoveryService(
        templateScanner,
        mock(FragmentDefinitionParser.class),
        mock(FragmentDomainService.class),
        mock(FragmentSignatureParser.class),
        new StructuredTemplateParser(),
        new FragmentExpressionParser(),
        mock(ThymeleafletCacheManager.class)
    );

    @Test
    void findTemplateParserDiagnostics_shouldReturnDynamicFragmentReferenceWarnings() throws IOException {
        when(templateScanner.scanTemplates()).thenReturn(List.of(
            new TemplateScanner.TemplateResource(
                "components/profile",
                """
                    <section>
                      <div th:replace="${dynamicReference}"></div>
                      <div th:insert="~{components/card}"></div>
                    </section>
                    """,
                "classpath:/templates/components/profile.html"
            )
        ));

        List<ParserDiagnostic> diagnostics = discoveryService.findTemplateParserDiagnostics("components/profile");

        assertThat(diagnostics)
            .anySatisfy(diagnostic -> {
                assertThat(diagnostic.code()).isEqualTo("TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED");
                assertThat(diagnostic.message()).contains("th:replace");
                assertThat(diagnostic.line()).isGreaterThan(0);
                assertThat(diagnostic.column()).isGreaterThan(0);
            })
            .anySatisfy(diagnostic -> {
                assertThat(diagnostic.code()).isEqualTo("FRAGMENT_EXPRESSION_MALFORMED");
                assertThat(diagnostic.message()).contains("components/card");
            });
    }
}
