package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class FragmentDefinitionParser {

    private static final Pattern FRAGMENT_PATTERN = Pattern.compile(
        "th:fragment\\s*=\\s*[\"']([^\"']+)[\"']"
    );

    public List<FragmentDefinition> parseTemplate(String templatePath, String content) {
        Objects.requireNonNull(templatePath, "templatePath cannot be null");
        Objects.requireNonNull(content, "content cannot be null");
        List<FragmentDefinition> definitions = new ArrayList<>();
        Matcher matcher = FRAGMENT_PATTERN.matcher(content);

        while (matcher.find()) {
            definitions.add(new FragmentDefinition(templatePath, matcher.group(1)));
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
