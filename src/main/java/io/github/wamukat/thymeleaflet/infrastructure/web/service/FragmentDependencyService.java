package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.outbound.FragmentDependencyPort;
import io.github.wamukat.thymeleaflet.domain.model.SecureTemplatePath;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResourcePathValidator;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * フラグメント内で利用している依存コンポーネントを抽出
 */
@Component
public class FragmentDependencyService implements FragmentDependencyPort {

    private static final Logger logger = LoggerFactory.getLogger(FragmentDependencyService.class);

    private static final Pattern FRAGMENT_DECL_PATTERN = Pattern.compile(
        "th:fragment\\s*=\\s*[\"']([^\"']+)[\"']"
    );
    private static final Pattern DEPENDENCY_ATTR_PATTERN = Pattern.compile(
        "th:(?:replace|include|insert)\\s*=\\s*"
    );

    private final ResolvedStorybookConfig storybookConfig;

    private final ResourcePathValidator resourcePathValidator;

    private final Map<String, List<DependencyComponent>> dependencyCache = new ConcurrentHashMap<>();

    public FragmentDependencyService(
        ResolvedStorybookConfig storybookConfig,
        ResourcePathValidator resourcePathValidator
    ) {
        this.storybookConfig = storybookConfig;
        this.resourcePathValidator = resourcePathValidator;
    }

    public List<DependencyComponent> findDependencies(String templatePath, String fragmentName) {
        if (storybookConfig.getCache().isEnabled()) {
            String cacheKey = templatePath + "::" + fragmentName;
            List<DependencyComponent> cached = dependencyCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        try {
            Resource resource = resourcePathValidator.findTemplate(
                templatePath,
                storybookConfig.getResources().getTemplatePaths()
            );

            if (!resource.exists()) {
                return List.of();
            }

            String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String target = extractFragmentBlock(html, fragmentName).orElse(html);

            Map<String, DependencyComponent> dependencies = new LinkedHashMap<>();
            for (String expression : extractDependencyExpressions(target)) {
                Optional<DependencyComponent> component = parseDependency(expression);
                if (component.isEmpty()) {
                    continue;
                }
                DependencyComponent resolvedComponent = component.orElseThrow();
                if (resolvedComponent.templatePath().equals(templatePath) && resolvedComponent.fragmentName().equals(fragmentName)) {
                    continue;
                }
                dependencies.put(resolvedComponent.key(), resolvedComponent);
            }

            List<DependencyComponent> result = new ArrayList<>(dependencies.values());
            if (storybookConfig.getCache().isEnabled()) {
                dependencyCache.put(templatePath + "::" + fragmentName, List.copyOf(result));
            }
            return result;
        } catch (Exception e) {
            logger.warn("Failed to extract dependencies for {}::{}: {}", templatePath, fragmentName, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<FragmentDependencyPort.DependencyComponent> findDependenciesForView(String templatePath, String fragmentName) {
        return findDependencies(templatePath, fragmentName).stream()
            .map(component -> new FragmentDependencyPort.DependencyComponent(
                component.templatePath(),
                component.fragmentName(),
                component.encodedTemplatePath()
            ))
            .toList();
    }

    private Optional<DependencyComponent> parseDependency(String expression) {
        String[] parts = expression.split("::");
        if (parts.length < 2) {
            return Optional.empty();
        }
        String templatePath = parts[0].trim();
        String fragmentPart = parts[1].trim();
        String fragmentName = fragmentPart.split("\\(")[0].trim();

        if (templatePath.isEmpty() || fragmentName.isEmpty()) {
            return Optional.empty();
        }

        String encodedPath = SecureTemplatePath.createUnsafe(templatePath.replace("/", ".")).forUrl();
        return Optional.of(new DependencyComponent(templatePath, fragmentName, encodedPath));
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

    private Optional<String> extractFragmentBlock(String html, String fragmentName) {
        Matcher matcher = FRAGMENT_DECL_PATTERN.matcher(html);
        while (matcher.find()) {
            String definition = matcher.group(1).trim();
            String name = definition.split("\\(")[0].trim();
            if (!name.equals(fragmentName)) {
                continue;
            }

            int tagStart = html.lastIndexOf('<', matcher.start());
            if (tagStart == -1) {
                return Optional.empty();
            }
            int tagEnd = html.indexOf('>', matcher.end());
            if (tagEnd == -1) {
                return Optional.empty();
            }

            Optional<String> tagName = extractTagName(html.substring(tagStart, tagEnd + 1));
            if (tagName.isEmpty()) {
                return Optional.empty();
            }
            String resolvedTagName = tagName.orElseThrow();

            int depth = 0;
            int index = tagStart;
            while (index < html.length()) {
                int nextOpen = html.indexOf("<" + resolvedTagName, index);
                int nextClose = html.indexOf("</" + resolvedTagName, index);

                if (nextClose == -1) {
                    return Optional.of(html.substring(tagStart));
                }

                if (nextOpen != -1 && nextOpen < nextClose) {
                    depth++;
                    index = nextOpen + resolvedTagName.length() + 1;
                    continue;
                }

                depth--;
                int closeEnd = html.indexOf('>', nextClose);
                if (closeEnd == -1) {
                    return Optional.of(html.substring(tagStart));
                }
                index = closeEnd + 1;
                if (depth <= 0) {
                    return Optional.of(html.substring(tagStart, index));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractTagName(String tag) {
        Matcher matcher = Pattern.compile("<\\s*([a-zA-Z0-9:-]+)").matcher(tag);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    public record DependencyComponent(String templatePath, String fragmentName, String encodedTemplatePath) {
        public String key() {
            return templatePath + "::" + fragmentName;
        }
    }
}
