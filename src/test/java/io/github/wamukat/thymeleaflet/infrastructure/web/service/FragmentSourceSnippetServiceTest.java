package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class FragmentSourceSnippetServiceTest {

    private final FragmentSourceSnippetService service =
        new FragmentSourceSnippetService(new DefaultResourceLoader());

    @Test
    void shouldIncludeWholeFragmentBlockForLongFragment() {
        String snippet = service.resolveSnippet("fragments/source-snippet-sample", "longFragment")
            .orElseThrow();

        assertThat(snippet).contains("th:fragment=\"longFragment\"");
        assertThat(snippet).contains("<div>line 25</div>");
        assertThat(snippet).contains("</section>");
    }

    @Test
    void shouldResolveSingleLineFragment() {
        String snippet = service.resolveSnippet("fragments/source-snippet-sample", "shortFragment")
            .orElseThrow();

        assertThat(snippet).contains("th:fragment=\"shortFragment\"");
        assertThat(snippet).contains("short");
    }
}
