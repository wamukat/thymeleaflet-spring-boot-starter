package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentSignatureParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * フラグメント実装コード（テンプレート抜粋）を取得するサービス。
 */
@Component
public class FragmentSourceSnippetService {

    private static final int CONTEXT_LINES_BEFORE = 4;
    private static final int CONTEXT_LINES_AFTER = 20;
    private static final Pattern OPEN_TAG_PATTERN = Pattern.compile("<\\s*([a-zA-Z][\\w:-]*)\\b");

    private final ResourceLoader resourceLoader;
    private final FragmentSignatureParser fragmentSignatureParser;

    @Autowired
    public FragmentSourceSnippetService(ResourceLoader resourceLoader) {
        this(resourceLoader, new FragmentSignatureParser());
    }

    FragmentSourceSnippetService(ResourceLoader resourceLoader, FragmentSignatureParser fragmentSignatureParser) {
        this.resourceLoader = resourceLoader;
        this.fragmentSignatureParser = fragmentSignatureParser;
    }

    public Optional<String> resolveSnippet(String templatePath, String fragmentName) {
        String templateSource = readTemplateSource(templatePath);
        if (templateSource.isEmpty()) {
            return Optional.empty();
        }

        SourceDocument document = SourceDocument.of(templateSource);
        int targetLine = findFragmentDefinitionLine(document, fragmentName);
        if (targetLine < 0) {
            return Optional.empty();
        }

        int openTagLine = findOpeningTagLine(document, targetLine);
        int start = findSnippetStartLine(document, openTagLine);
        int endExclusive = findSnippetEndExclusive(document, openTagLine, targetLine);

        StringBuilder snippet = new StringBuilder();
        for (int i = start; i < endExclusive; i++) {
            snippet.append(String.format("%4d | %s%n", i + 1, document.line(i)));
        }
        return Optional.of(snippet.toString().trim());
    }

    private int findOpeningTagLine(SourceDocument document, int targetLine) {
        for (int i = targetLine; i >= 0; i--) {
            if (extractTagName(document.line(i)).isPresent()) {
                return i;
            }
            if (targetLine - i > 5) {
                break;
            }
        }
        return targetLine;
    }

    private int findSnippetStartLine(SourceDocument document, int openTagLine) {
        return findLeadingCommentBlockStart(document, openTagLine)
            .orElse(Math.max(0, openTagLine - CONTEXT_LINES_BEFORE));
    }

    private Optional<Integer> findLeadingCommentBlockStart(SourceDocument document, int openTagLine) {
        int cursor = document.previousNonBlankLine(openTagLine - 1);
        int earliestCommentStart = -1;

        while (cursor >= 0) {
            if (document.line(cursor).contains("-->")) {
                int commentStart = findHtmlCommentStart(document, cursor);
                if (commentStart < 0) {
                    break;
                }
                earliestCommentStart = commentStart;
                cursor = document.previousNonBlankLine(commentStart - 1);
            } else if (isJavaLineDocComment(document.line(cursor))) {
                int commentStart = findJavaLineDocCommentStart(document, cursor);
                earliestCommentStart = commentStart;
                cursor = document.previousNonBlankLine(commentStart - 1);
            } else {
                break;
            }
        }

        return earliestCommentStart >= 0 ? Optional.of(earliestCommentStart) : Optional.empty();
    }

    private int findHtmlCommentStart(SourceDocument document, int commentEndLine) {
        for (int i = commentEndLine; i >= 0; i--) {
            if (document.line(i).contains("<!--")) {
                return i;
            }
        }
        return -1;
    }

    private int findJavaLineDocCommentStart(SourceDocument document, int commentEndLine) {
        int start = commentEndLine;
        for (int i = commentEndLine - 1; i >= 0; i--) {
            if (!isJavaLineDocComment(document.line(i))) {
                break;
            }
            start = i;
        }
        return start;
    }

    private boolean isJavaLineDocComment(String line) {
        return line.trim().startsWith("///");
    }

    private int findSnippetEndExclusive(SourceDocument document, int openTagLine, int targetLine) {
        Optional<String> tagNameOptional = extractTagName(document.line(openTagLine));
        if (tagNameOptional.isEmpty()) {
            return Math.min(document.size(), targetLine + CONTEXT_LINES_AFTER + 1);
        }

        String tagName = tagNameOptional.orElseThrow();
        int depth = 0;
        boolean entered = false;

        for (int i = openTagLine; i < document.size(); i++) {
            String line = document.line(i);
            int openingCount = countOpeningTags(line, tagName);
            int closingCount = countClosingTags(line, tagName);
            int selfClosingCount = countSelfClosingTags(line, tagName);

            if (openingCount > 0) {
                entered = true;
            }

            depth += openingCount;
            depth -= selfClosingCount;
            depth -= closingCount;

            if (entered && depth <= 0) {
                return i + 1;
            }
        }

        return Math.min(document.size(), targetLine + CONTEXT_LINES_AFTER + 1);
    }

    private Optional<String> extractTagName(String line) {
        Matcher matcher = OPEN_TAG_PATTERN.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String tagName = matcher.group(1);
        if (tagName.startsWith("/")) {
            return Optional.empty();
        }
        return Optional.of(tagName);
    }

    private int countOpeningTags(String line, String tagName) {
        return countMatches(line, "<\\s*" + Pattern.quote(tagName) + "(?=\\s|>|/)");
    }

    private int countClosingTags(String line, String tagName) {
        return countMatches(line, "</\\s*" + Pattern.quote(tagName) + "\\s*>");
    }

    private int countSelfClosingTags(String line, String tagName) {
        return countMatches(line, "<\\s*" + Pattern.quote(tagName) + "(?=\\s|>)[^>]*?/\\s*>");
    }

    private int countMatches(String line, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(line);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private int findFragmentDefinitionLine(SourceDocument document, String fragmentName) {
        for (int i = 0; i < document.size(); i++) {
            Optional<String> definition = extractFragmentDefinition(document, i);
            if (definition.isEmpty()) {
                continue;
            }
            if (matchesFragmentName(definition.orElseThrow(), fragmentName)) {
                return i;
            }
        }
        return -1;
    }

    private Optional<String> extractFragmentDefinition(SourceDocument document, int lineIndex) {
        String line = document.line(lineIndex);
        int attributeIndex = fragmentAttributeIndex(line);
        if (attributeIndex < 0) {
            return Optional.empty();
        }
        int equalsIndex = line.indexOf('=', attributeIndex);
        if (equalsIndex < 0) {
            return Optional.empty();
        }
        return readQuotedAttributeValue(document, lineIndex, equalsIndex + 1);
    }

    private int fragmentAttributeIndex(String line) {
        int thIndex = attributeNameIndex(line, "th:fragment");
        int dataThIndex = attributeNameIndex(line, "data-th-fragment");
        if (thIndex < 0) {
            return dataThIndex;
        }
        if (dataThIndex < 0) {
            return thIndex;
        }
        return Math.min(thIndex, dataThIndex);
    }

    private int attributeNameIndex(String line, String attributeName) {
        int searchFrom = 0;
        boolean inQuotedValue = false;
        char quote = 0;
        while (searchFrom < line.length()) {
            int index = line.indexOf(attributeName, searchFrom);
            if (index < 0) {
                return -1;
            }
            QuoteState quoteState = quoteStateUntil(line, searchFrom, index, inQuotedValue, quote);
            inQuotedValue = quoteState.inQuotedValue();
            quote = quoteState.quote();
            if (!inQuotedValue && isAttributeNameBoundary(line, index, attributeName.length())) {
                return index;
            }
            searchFrom = index + attributeName.length();
        }
        return -1;
    }

    private QuoteState quoteStateUntil(
        String line,
        int startColumn,
        int endColumn,
        boolean initialInQuotedValue,
        char initialQuote
    ) {
        boolean inQuotedValue = initialInQuotedValue;
        char quote = initialQuote;
        for (int i = startColumn; i < endColumn; i++) {
            char current = line.charAt(i);
            if (inQuotedValue) {
                if (current == quote) {
                    inQuotedValue = false;
                    quote = 0;
                }
            } else if (current == '"' || current == '\'') {
                inQuotedValue = true;
                quote = current;
            }
        }
        return new QuoteState(inQuotedValue, quote);
    }

    private boolean isAttributeNameBoundary(String line, int startIndex, int attributeNameLength) {
        int previous = startIndex - 1;
        if (previous >= 0 && isAttributeNamePart(line.charAt(previous))) {
            return false;
        }

        int cursor = startIndex + attributeNameLength;
        while (cursor < line.length() && Character.isWhitespace(line.charAt(cursor))) {
            cursor++;
        }
        return cursor < line.length() && line.charAt(cursor) == '=';
    }

    private boolean isAttributeNamePart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '-' || value == ':';
    }

    private Optional<String> readQuotedAttributeValue(SourceDocument document, int startLine, int startColumn) {
        boolean reading = false;
        char quote = 0;
        StringBuilder value = new StringBuilder();

        for (int lineIndex = startLine; lineIndex < document.size(); lineIndex++) {
            String line = document.line(lineIndex);
            int column = lineIndex == startLine ? startColumn : 0;
            while (column < line.length()) {
                char current = line.charAt(column);
                if (!reading) {
                    if (current == '"' || current == '\'') {
                        reading = true;
                        quote = current;
                    }
                    column++;
                    continue;
                }
                if (current == quote) {
                    return Optional.of(value.toString());
                }
                value.append(current);
                column++;
            }
            if (reading) {
                value.append('\n');
            }
        }
        return Optional.empty();
    }

    private boolean matchesFragmentName(String definition, String fragmentName) {
        FragmentSignatureParser.ParseResult result = fragmentSignatureParser.parse(definition);
        if (result instanceof FragmentSignatureParser.ParseSuccess success) {
            return success.fragmentName().equals(fragmentName);
        }
        return false;
    }

    private String readTemplateSource(String templatePath) {
        Resource resource = resourceLoader.getResource("classpath:templates/" + templatePath + ".html");
        if (!resource.exists()) {
            return "";
        }
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            return "";
        }
    }

    private record SourceDocument(List<String> lines) {

        private static SourceDocument of(String source) {
            return new SourceDocument(List.of(source.split("\\R", -1)));
        }

        private String line(int index) {
            return lines.get(index);
        }

        private int size() {
            return lines.size();
        }

        private int previousNonBlankLine(int startLine) {
            for (int i = startLine; i >= 0; i--) {
                if (!lines.get(i).isBlank()) {
                    return i;
                }
            }
            return -1;
        }
    }

    private record QuoteState(boolean inQuotedValue, char quote) {}
}
