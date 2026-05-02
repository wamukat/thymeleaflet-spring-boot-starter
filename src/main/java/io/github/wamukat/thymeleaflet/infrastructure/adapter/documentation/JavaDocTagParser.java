package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaDocTagParser {

    private static final Pattern PARAM_PATTERN = Pattern.compile(
        "@param\\s+(\\w+)\\s+\\{@code\\s+([^}]+?)\\}\\s+\\[(required|optional(?:=[^\\]]*)?)\\]\\s+([\\s\\S]*)",
        Pattern.DOTALL
    );

    private static final Pattern MODEL_PATTERN = Pattern.compile(
        "@model\\s+([\\w.\\[\\]]+)\\s+\\{@code\\s+([^}]+?)\\}\\s+\\[(required|optional(?:=[^\\]]*)?)\\]\\s+([\\s\\S]*)",
        Pattern.DOTALL
    );

    private static final Pattern FRAGMENT_PATTERN = Pattern.compile(
        "@fragment\\s+([A-Za-z_][\\w-]*)",
        Pattern.MULTILINE
    );

    private static final Pattern BACKGROUND_PATTERN = Pattern.compile(
        "@background\\s+(\\S+)",
        Pattern.MULTILINE
    );

    private static final Pattern VALUES_PATTERN = Pattern.compile(
        "[。.]?\\s*values:\\s*(.+?)(?=\\s*$)",
        Pattern.MULTILINE
    );

    private static final Pattern VALUE_PATTERN = Pattern.compile("\"([^\"]+)\"");

    ParsedTags parse(String javadocContent) {
        List<String> lines = normalizeJavadocLines(javadocContent);
        List<String> tagBlocks = collectTagBlocks(lines);
        return new ParsedTags(
            extractDescription(lines),
            parseParameters(tagBlocks),
            parseModels(tagBlocks),
            parseFragmentName(tagBlocks),
            parseBackgroundColor(tagBlocks)
        );
    }

    private List<JavaDocAnalyzer.ParameterInfo> parseParameters(List<String> tagBlocks) {
        List<JavaDocAnalyzer.ParameterInfo> parameters = new ArrayList<>();

        for (String tagBlock : tagBlocks) {
            Matcher paramMatcher = PARAM_PATTERN.matcher(tagBlock);
            if (!paramMatcher.matches()) {
                continue;
            }
            String name = paramMatcher.group(1);
            String type = paramMatcher.group(2);
            String requiredOrOptional = paramMatcher.group(3);
            ParsedDescription parsedDescription = parseDescriptionWithAllowedValues(paramMatcher.group(4));

            parameters.add(JavaDocAnalyzer.ParameterInfo.of(
                name,
                type,
                "required".equals(requiredOrOptional),
                parseDefaultValue(requiredOrOptional),
                Optional.of(parsedDescription.description()),
                parsedDescription.allowedValues()
            ));
        }

        return parameters;
    }

    private List<JavaDocAnalyzer.ModelInfo> parseModels(List<String> tagBlocks) {
        List<JavaDocAnalyzer.ModelInfo> models = new ArrayList<>();

        for (String tagBlock : tagBlocks) {
            Matcher modelMatcher = MODEL_PATTERN.matcher(tagBlock);
            if (!modelMatcher.matches()) {
                continue;
            }
            String name = modelMatcher.group(1);
            String type = modelMatcher.group(2);
            String requiredOrOptional = modelMatcher.group(3);
            String description = normalizeDescription(modelMatcher.group(4));

            models.add(JavaDocAnalyzer.ModelInfo.of(
                name,
                type,
                "required".equals(requiredOrOptional),
                parseDefaultValue(requiredOrOptional),
                Optional.of(description)
            ));
        }

        return models;
    }

    private Optional<String> parseDefaultValue(String requiredOrOptional) {
        if (!requiredOrOptional.startsWith("optional=")) {
            return Optional.empty();
        }
        String rawDefaultValue = requiredOrOptional.substring("optional=".length());
        if ("null".equals(rawDefaultValue)) {
            return Optional.empty();
        }
        return Optional.of(rawDefaultValue);
    }

    private ParsedDescription parseDescriptionWithAllowedValues(String fullDescription) {
        Matcher valuesMatcher = VALUES_PATTERN.matcher(fullDescription);
        if (!valuesMatcher.find()) {
            return new ParsedDescription(normalizeDescription(fullDescription), Collections.emptyList());
        }

        String valuesStr = valuesMatcher.group(1);
        String description = fullDescription.substring(0, valuesMatcher.start()).trim();
        return new ParsedDescription(normalizeDescription(description), parseAllowedValues(valuesStr));
    }

    private List<String> parseAllowedValues(String valuesStr) {
        if (valuesStr.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        Matcher valueMatcher = VALUE_PATTERN.matcher(valuesStr);
        while (valueMatcher.find()) {
            values.add(valueMatcher.group(1));
        }

        return values;
    }

    private Optional<String> parseFragmentName(List<String> tagBlocks) {
        for (String tagBlock : tagBlocks) {
            Matcher fragmentMatcher = FRAGMENT_PATTERN.matcher(tagBlock);
            if (fragmentMatcher.find()) {
                return Optional.of(fragmentMatcher.group(1));
            }
        }
        return Optional.empty();
    }

    private Optional<String> parseBackgroundColor(List<String> tagBlocks) {
        for (String tagBlock : tagBlocks) {
            Matcher backgroundMatcher = BACKGROUND_PATTERN.matcher(tagBlock);
            if (backgroundMatcher.find()) {
                return Optional.of(backgroundMatcher.group(1));
            }
        }
        return Optional.empty();
    }

    private String extractDescription(List<String> lines) {
        StringBuilder description = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("@")) {
                break;
            }

            if (!line.isEmpty()) {
                if (!description.isEmpty()) {
                    description.append("\n");
                }
                description.append(line);
            }
        }

        return description.toString();
    }

    private List<String> collectTagBlocks(List<String> lines) {
        List<String> tagBlocks = new ArrayList<>();
        StringBuilder currentTag = new StringBuilder();
        boolean collectingTag = false;

        for (String line : lines) {
            if (line.startsWith("@")) {
                addTagBlockIfPresent(tagBlocks, currentTag);
                currentTag.setLength(0);
                currentTag.append(line);
                collectingTag = true;
                continue;
            }
            if (!collectingTag || line.isBlank()) {
                continue;
            }
            currentTag.append('\n').append(line);
        }

        addTagBlockIfPresent(tagBlocks, currentTag);
        return tagBlocks;
    }

    private void addTagBlockIfPresent(List<String> tagBlocks, StringBuilder currentTag) {
        if (!currentTag.isEmpty()) {
            tagBlocks.add(currentTag.toString());
        }
    }

    private List<String> normalizeJavadocLines(String javadocContent) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : javadocContent.split("\n")) {
            lines.add(normalizeJavadocLine(rawLine));
        }
        return lines;
    }

    private String normalizeJavadocLine(String rawLine) {
        String line = rawLine.trim();
        if (line.startsWith("*")) {
            return line.substring(1).trim();
        }
        return line;
    }

    private String normalizeDescription(String description) {
        return description.replaceAll("\\s+", " ").trim();
    }

    record ParsedTags(
        String description,
        List<JavaDocAnalyzer.ParameterInfo> parameters,
        List<JavaDocAnalyzer.ModelInfo> models,
        Optional<String> fragmentName,
        Optional<String> backgroundColor
    ) {
    }

    private record ParsedDescription(String description, List<String> allowedValues) {
    }
}
