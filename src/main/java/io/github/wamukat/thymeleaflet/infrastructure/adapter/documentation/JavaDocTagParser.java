package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaDocTagParser {

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
            Optional<ParsedFieldTag> parsedTag = parseFieldTag(tagBlock, "param");
            if (parsedTag.isEmpty()) {
                continue;
            }
            ParsedFieldTag fieldTag = parsedTag.get();
            ParsedDescription parsedDescription = parseDescriptionWithAllowedValues(fieldTag.description());

            parameters.add(JavaDocAnalyzer.ParameterInfo.of(
                fieldTag.identifier(),
                fieldTag.type(),
                fieldTag.required(),
                fieldTag.defaultValue(),
                Optional.of(parsedDescription.description()),
                parsedDescription.allowedValues()
            ));
        }

        return parameters;
    }

    private List<JavaDocAnalyzer.ModelInfo> parseModels(List<String> tagBlocks) {
        List<JavaDocAnalyzer.ModelInfo> models = new ArrayList<>();

        for (String tagBlock : tagBlocks) {
            Optional<ParsedFieldTag> parsedTag = parseFieldTag(tagBlock, "model");
            if (parsedTag.isEmpty()) {
                continue;
            }
            ParsedFieldTag fieldTag = parsedTag.get();

            models.add(JavaDocAnalyzer.ModelInfo.of(
                fieldTag.identifier(),
                fieldTag.type(),
                fieldTag.required(),
                fieldTag.defaultValue(),
                Optional.of(normalizeDescription(fieldTag.description()))
            ));
        }

        return models;
    }

    private Optional<ParsedFieldTag> parseFieldTag(String tagBlock, String expectedTagName) {
        TagCursor cursor = new TagCursor(tagBlock);
        Optional<String> tagName = cursor.readTagName();
        if (tagName.isEmpty() || !expectedTagName.equals(tagName.get())) {
            return Optional.empty();
        }
        Optional<String> identifier = cursor.readIdentifier();
        Optional<String> type = cursor.readCodeLiteral();
        Optional<ParsedMarker> marker = cursor.readMarker();
        if (identifier.isEmpty() || type.isEmpty() || marker.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedFieldTag(
            identifier.get(),
            type.get(),
            marker.get().required(),
            marker.get().defaultValue(),
            cursor.remainingDescription()
        ));
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
            line = line.substring(1).trim();
        }
        if (line.equals("/")) {
            return "";
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

    private record ParsedFieldTag(
        String identifier,
        String type,
        boolean required,
        Optional<String> defaultValue,
        String description
    ) {
    }

    private record ParsedMarker(boolean required, Optional<String> defaultValue) {
    }

    private static final class TagCursor {
        private static final String CODE_PREFIX = "{@code";
        private final String source;
        private int index;

        private TagCursor(String source) {
            this.source = source;
        }

        private Optional<String> readTagName() {
            skipWhitespace();
            if (!consume('@')) {
                return Optional.empty();
            }
            return readUntilWhitespace()
                .filter(tagName -> !tagName.isBlank());
        }

        private Optional<String> readIdentifier() {
            skipWhitespace();
            return readUntilWhitespace()
                .filter(identifier -> !identifier.isBlank());
        }

        private Optional<String> readCodeLiteral() {
            skipWhitespace();
            if (!source.startsWith(CODE_PREFIX, index)) {
                return Optional.empty();
            }
            index += CODE_PREFIX.length();
            int end = source.indexOf('}', index);
            if (end < 0) {
                return Optional.empty();
            }
            String codeLiteral = source.substring(index, end).trim();
            index = end + 1;
            if (codeLiteral.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(codeLiteral);
        }

        private Optional<ParsedMarker> readMarker() {
            skipWhitespace();
            if (!consume('[')) {
                return Optional.empty();
            }
            int end = source.indexOf(']', index);
            if (end < 0) {
                return Optional.empty();
            }
            String marker = source.substring(index, end).trim();
            index = end + 1;
            if ("required".equals(marker)) {
                return Optional.of(new ParsedMarker(true, Optional.empty()));
            }
            if ("optional".equals(marker)) {
                return Optional.of(new ParsedMarker(false, Optional.empty()));
            }
            if (marker.startsWith("optional=")) {
                String defaultValue = marker.substring("optional=".length());
                if (defaultValue.isBlank() || "null".equals(defaultValue)) {
                    return Optional.of(new ParsedMarker(false, Optional.empty()));
                }
                return Optional.of(new ParsedMarker(false, Optional.of(defaultValue)));
            }
            return Optional.empty();
        }

        private String remainingDescription() {
            return source.substring(index).trim();
        }

        private Optional<String> readUntilWhitespace() {
            int start = index;
            while (index < source.length() && !Character.isWhitespace(source.charAt(index))) {
                index++;
            }
            if (start == index) {
                return Optional.empty();
            }
            return Optional.of(source.substring(start, index));
        }

        private boolean consume(char expected) {
            if (index >= source.length() || source.charAt(index) != expected) {
                return false;
            }
            index++;
            return true;
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }
    }
}
