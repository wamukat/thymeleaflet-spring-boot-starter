package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class FragmentDefinitionParserTest {

    private final FragmentDefinitionParser parser = new FragmentDefinitionParser();

    @Test
    void parseTemplate_shouldExtractFragmentDefinitionsInDeclarationOrder() {
        String content = """
            <section th:fragment="card(title, body)">Card</section>
            <footer th:fragment='footer'>Footer</footer>
            """;

        List<FragmentDefinitionParser.FragmentDefinition> definitions = parser.parseTemplate(
            "components/card",
            content
        );

        assertThat(definitions)
            .extracting(FragmentDefinitionParser.FragmentDefinition::definition)
            .containsExactly("card(title, body)", "footer");
        assertThat(definitions)
            .extracting(FragmentDefinitionParser.FragmentDefinition::templatePath)
            .containsExactly("components/card", "components/card");
    }

    @Test
    void parseTemplate_shouldReturnEmptyListWhenTemplateHasNoFragments() {
        List<FragmentDefinitionParser.FragmentDefinition> definitions = parser.parseTemplate(
            "pages/home",
            "<main>No fragments</main>"
        );

        assertThat(definitions).isEmpty();
    }
}
