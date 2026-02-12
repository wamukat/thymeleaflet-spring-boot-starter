package io.github.wamukat.thymeleaflet.infrastructure.web.service;

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

    public FragmentSourceSnippetService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Optional<String> resolveSnippet(String templatePath, String fragmentName) {
        String templateSource = readTemplateSource(templatePath);
        if (templateSource.isEmpty()) {
            return Optional.empty();
        }

        List<String> lines = List.of(templateSource.split("\\R", -1));
        int targetLine = findFragmentDefinitionLine(lines, fragmentName);
        if (targetLine < 0) {
            return Optional.empty();
        }

        int openTagLine = findOpeningTagLine(lines, targetLine);
        int start = Math.max(0, openTagLine - CONTEXT_LINES_BEFORE);
        int endExclusive = findSnippetEndExclusive(lines, openTagLine, targetLine);

        StringBuilder snippet = new StringBuilder();
        for (int i = start; i < endExclusive; i++) {
            snippet.append(String.format("%4d | %s%n", i + 1, lines.get(i)));
        }
        return Optional.of(snippet.toString().trim());
    }

    private int findOpeningTagLine(List<String> lines, int targetLine) {
        for (int i = targetLine; i >= 0; i--) {
            if (extractTagName(lines.get(i)).isPresent()) {
                return i;
            }
            if (targetLine - i > 5) {
                break;
            }
        }
        return targetLine;
    }

    private int findSnippetEndExclusive(List<String> lines, int openTagLine, int targetLine) {
        Optional<String> tagNameOptional = extractTagName(lines.get(openTagLine));
        if (tagNameOptional.isEmpty()) {
            return Math.min(lines.size(), targetLine + CONTEXT_LINES_AFTER + 1);
        }

        String tagName = tagNameOptional.orElseThrow();
        int depth = 0;
        boolean entered = false;

        for (int i = openTagLine; i < lines.size(); i++) {
            String line = lines.get(i);
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

        return Math.min(lines.size(), targetLine + CONTEXT_LINES_AFTER + 1);
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

    private int findFragmentDefinitionLine(List<String> lines, String fragmentName) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.contains("th:fragment")) {
                continue;
            }
            if (line.contains("\"" + fragmentName)
                || line.contains("'" + fragmentName)
                || line.contains(fragmentName + "(")
                || line.contains(fragmentName + " ")) {
                return i;
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            String current = lines.get(i);
            String next = (i + 1 < lines.size()) ? lines.get(i + 1) : "";
            String previous = (i > 0) ? lines.get(i - 1) : "";
            String window = previous + "\n" + current + "\n" + next;
            if (window.contains("th:fragment") && window.contains(fragmentName)) {
                return i;
            }
        }
        return -1;
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
}
