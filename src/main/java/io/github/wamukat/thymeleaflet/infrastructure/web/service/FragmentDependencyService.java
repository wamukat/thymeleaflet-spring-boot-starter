package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.domain.model.SecureTemplatePath;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResourcePathValidator;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * フラグメント内で利用している依存コンポーネントを抽出
 */
@Component
public class FragmentDependencyService {

    private static final Logger logger = LoggerFactory.getLogger(FragmentDependencyService.class);

    private static final Pattern FRAGMENT_DECL_PATTERN = Pattern.compile(
        "th:fragment\\s*=\\s*[\"']([^\"']+)[\"']"
    );
    private static final Pattern DEPENDENCY_ATTR_PATTERN = Pattern.compile(
        "th:(?:replace|include|insert)\\s*=\\s*"
    );

    @Autowired
    private StorybookProperties storybookProperties;

    @Autowired
    private ResourcePathValidator resourcePathValidator;

    private final Map<String, List<DependencyComponent>> dependencyCache = new ConcurrentHashMap<>();

    public List<DependencyComponent> findDependencies(String templatePath, String fragmentName) {
        if (storybookProperties.getCache().isEnabled()) {
            String cacheKey = templatePath + "::" + fragmentName;
            List<DependencyComponent> cached = dependencyCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        try {
            Resource resource = resourcePathValidator.findTemplate(
                templatePath,
                storybookProperties.getResources().getTemplatePaths()
            );

            if (!resource.exists()) {
                return List.of();
            }

            String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            @Nullable String fragmentBlock = extractFragmentBlock(html, fragmentName);
            String target = fragmentBlock != null ? fragmentBlock : html;

            Map<String, DependencyComponent> dependencies = new LinkedHashMap<>();
            for (String expression : extractDependencyExpressions(target)) {
                @Nullable DependencyComponent component = parseDependency(expression);
                if (component == null) {
                    continue;
                }
                if (component.templatePath().equals(templatePath) && component.fragmentName().equals(fragmentName)) {
                    continue;
                }
                dependencies.put(component.key(), component);
            }

            List<DependencyComponent> result = new ArrayList<>(dependencies.values());
            if (storybookProperties.getCache().isEnabled()) {
                dependencyCache.put(templatePath + "::" + fragmentName, List.copyOf(result));
            }
            return result;
        } catch (Exception e) {
            logger.warn("Failed to extract dependencies for {}::{}: {}", templatePath, fragmentName, e.getMessage());
            return List.of();
        }
    }

    private @Nullable DependencyComponent parseDependency(String expression) {
        String[] parts = expression.split("::");
        if (parts.length < 2) {
            return null;
        }
        String templatePath = parts[0].trim();
        String fragmentPart = parts[1].trim();
        String fragmentName = fragmentPart.split("\\(")[0].trim();

        if (templatePath.isEmpty() || fragmentName.isEmpty()) {
            return null;
        }

        String encodedPath = SecureTemplatePath.createUnsafe(templatePath.replace("/", ".")).forUrl();
        return new DependencyComponent(templatePath, fragmentName, encodedPath);
    }

    private List<String> extractDependencyExpressions(String html) {
        List<String> expressions = new ArrayList<>();
        Matcher matcher = DEPENDENCY_ATTR_PATTERN.matcher(html);
        while (matcher.find()) {
            int index = matcher.end();
            if (index >= html.length()) {
                continue;
            }
            char quote = html.charAt(index);
            if (quote != '"' && quote != '\'') {
                continue;
            }
            int valueStart = index + 1;
            int valueEnd = html.indexOf(quote, valueStart);
            if (valueEnd == -1) {
                continue;
            }
            String rawValue = html.substring(valueStart, valueEnd).trim();
            if (rawValue.startsWith("~{") && rawValue.endsWith("}")) {
                expressions.add(rawValue.substring(2, rawValue.length() - 1).trim());
            }
        }
        return expressions;
    }

    private @Nullable String extractFragmentBlock(String html, String fragmentName) {
        Matcher matcher = FRAGMENT_DECL_PATTERN.matcher(html);
        while (matcher.find()) {
            String definition = matcher.group(1).trim();
            String name = definition.split("\\(")[0].trim();
            if (!name.equals(fragmentName)) {
                continue;
            }

            int tagStart = html.lastIndexOf('<', matcher.start());
            if (tagStart == -1) {
                return null;
            }
            int tagEnd = html.indexOf('>', matcher.end());
            if (tagEnd == -1) {
                return null;
            }

            @Nullable String tagName = extractTagName(html.substring(tagStart, tagEnd + 1));
            if (tagName == null) {
                return null;
            }

            int depth = 0;
            int index = tagStart;
            while (index < html.length()) {
                int nextOpen = html.indexOf("<" + tagName, index);
                int nextClose = html.indexOf("</" + tagName, index);

                if (nextClose == -1) {
                    return html.substring(tagStart);
                }

                if (nextOpen != -1 && nextOpen < nextClose) {
                    depth++;
                    index = nextOpen + tagName.length() + 1;
                    continue;
                }

                depth--;
                int closeEnd = html.indexOf('>', nextClose);
                if (closeEnd == -1) {
                    return html.substring(tagStart);
                }
                index = closeEnd + 1;
                if (depth <= 0) {
                    return html.substring(tagStart, index);
                }
            }
        }
        return null;
    }

    private @Nullable String extractTagName(String tag) {
        Matcher matcher = Pattern.compile("<\\s*([a-zA-Z0-9:-]+)").matcher(tag);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public record DependencyComponent(String templatePath, String fragmentName, String encodedTemplatePath) {
        public String key() {
            return templatePath + "::" + fragmentName;
        }
    }
}
