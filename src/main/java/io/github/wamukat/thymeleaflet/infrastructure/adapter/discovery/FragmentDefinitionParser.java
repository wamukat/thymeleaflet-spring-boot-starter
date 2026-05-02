package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class FragmentDefinitionParser {

    private static final Set<String> FRAGMENT_ATTRIBUTES = Set.of("th:fragment", "data-th-fragment");

    private final StructuredTemplateParser templateParser;

    public FragmentDefinitionParser() {
        this(new StructuredTemplateParser());
    }

    FragmentDefinitionParser(StructuredTemplateParser templateParser) {
        this.templateParser = templateParser;
    }

    public List<FragmentDefinition> parseTemplate(String templatePath, String content) {
        Objects.requireNonNull(templatePath, "templatePath cannot be null");
        Objects.requireNonNull(content, "content cannot be null");
        List<FragmentDefinition> definitions = new ArrayList<>();
        StructuredTemplateParser.ParsedTemplate template = templateParser.parse(content);

        for (StructuredTemplateParser.TemplateElement element : template.elements()) {
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                String name = attribute.name().toLowerCase(Locale.ROOT);
                if (attribute.hasValue() && FRAGMENT_ATTRIBUTES.contains(name)) {
                    definitions.add(new FragmentDefinition(templatePath, attribute.value()));
                }
            }
        }

        return List.copyOf(definitions);
    }

    public record FragmentDefinition(String templatePath, String definition) {
        public FragmentDefinition {
            templatePath = templatePath.trim();
            definition = definition.trim();
        }
    }
}
