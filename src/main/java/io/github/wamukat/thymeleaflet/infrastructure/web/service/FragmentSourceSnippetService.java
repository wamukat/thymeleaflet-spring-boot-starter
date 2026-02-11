package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * フラグメント実装コード（テンプレート抜粋）を取得するサービス。
 */
@Component
public class FragmentSourceSnippetService {

    private static final int CONTEXT_LINES_BEFORE = 4;
    private static final int CONTEXT_LINES_AFTER = 20;

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

        int start = Math.max(0, targetLine - CONTEXT_LINES_BEFORE);
        int endExclusive = Math.min(lines.size(), targetLine + CONTEXT_LINES_AFTER + 1);

        StringBuilder snippet = new StringBuilder();
        for (int i = start; i < endExclusive; i++) {
            snippet.append(String.format("%4d | %s%n", i + 1, lines.get(i)));
        }
        return Optional.of(snippet.toString().trim());
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
