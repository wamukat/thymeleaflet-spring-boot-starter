package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.outbound.FragmentDependencyPort;
import io.github.wamukat.thymeleaflet.domain.model.SecureTemplatePath;
import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;
import io.github.wamukat.thymeleaflet.infrastructure.cache.ThymeleafletCacheManager;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResourcePathValidator;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * フラグメント内で利用している依存コンポーネントを抽出
 */
@Component
public class FragmentDependencyService implements FragmentDependencyPort {

    private static final Logger logger = LoggerFactory.getLogger(FragmentDependencyService.class);

    private static final Set<String> FRAGMENT_ATTRIBUTES = Set.of("th:fragment", "data-th-fragment");
    private static final Set<String> DEPENDENCY_ATTRIBUTES = Set.of(
        "th:replace", "th:include", "th:insert",
        "data-th-replace", "data-th-include", "data-th-insert"
    );

    private final ResolvedStorybookConfig storybookConfig;

    private final ResourcePathValidator resourcePathValidator;

    private final ThymeleafletCacheManager cacheManager;

    private final StructuredTemplateParser templateParser;

    @Autowired
    public FragmentDependencyService(
        ResolvedStorybookConfig storybookConfig,
        ResourcePathValidator resourcePathValidator,
        ThymeleafletCacheManager cacheManager
    ) {
        this(storybookConfig, resourcePathValidator, cacheManager, new StructuredTemplateParser());
    }

    FragmentDependencyService(
        ResolvedStorybookConfig storybookConfig,
        ResourcePathValidator resourcePathValidator,
        ThymeleafletCacheManager cacheManager,
        StructuredTemplateParser templateParser
    ) {
        this.storybookConfig = storybookConfig;
        this.resourcePathValidator = resourcePathValidator;
        this.cacheManager = cacheManager;
        this.templateParser = templateParser;
    }

    public List<DependencyComponent> findDependencies(String templatePath, String fragmentName) {
        String cacheKey = templatePath + "::" + fragmentName;
        Optional<List<DependencyComponent>> cached = cacheManager.get("fragment-dependencies", cacheKey);
        if (cached.isPresent()) {
            return cached.orElseThrow();
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
            StructuredTemplateParser.ParsedTemplate template = templateParser.parse(html);
            List<StructuredTemplateParser.TemplateElement> targetElements = elementsForFragment(template, fragmentName);

            Map<String, DependencyComponent> dependencies = new LinkedHashMap<>();
            for (String expression : extractDependencyExpressions(targetElements)) {
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
            List<DependencyComponent> immutableResult = List.copyOf(result);
            cacheManager.put("fragment-dependencies", cacheKey, immutableResult);
            return immutableResult;
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
        String normalizedExpression = unwrapFragmentExpression(expression);
        String[] parts = normalizedExpression.split("::", 2);
        if (parts.length < 2) {
            return Optional.empty();
        }
        String templatePath = unquote(parts[0].trim());
        String fragmentPart = parts[1].trim();
        String fragmentName = fragmentPart.split("\\(")[0].trim();

        if (templatePath.isEmpty() || fragmentName.isEmpty()) {
            return Optional.empty();
        }

        String encodedPath = SecureTemplatePath.createUnsafe(templatePath.replace("/", ".")).forUrl();
        return Optional.of(new DependencyComponent(templatePath, fragmentName, encodedPath));
    }

    private List<String> extractDependencyExpressions(List<StructuredTemplateParser.TemplateElement> elements) {
        List<String> expressions = new ArrayList<>();
        for (StructuredTemplateParser.TemplateElement element : elements) {
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                String name = attribute.name().toLowerCase(Locale.ROOT);
                if (!attribute.hasValue() || !DEPENDENCY_ATTRIBUTES.contains(name)) {
                    continue;
                }
                expressions.add(attribute.value().trim());
            }
        }
        return expressions;
    }

    private List<StructuredTemplateParser.TemplateElement> elementsForFragment(
        StructuredTemplateParser.ParsedTemplate template,
        String fragmentName
    ) {
        return template.elementsMatchingSubtree(element ->
            fragmentDefinition(element)
                .map(definition -> definition.split("\\(", 2)[0].trim())
                .filter(fragmentName::equals)
                .isPresent()
        );
    }

    private Optional<String> fragmentDefinition(StructuredTemplateParser.TemplateElement element) {
        return element.attributes().stream()
            .filter(StructuredTemplateParser.TemplateAttribute::hasValue)
            .filter(attribute -> FRAGMENT_ATTRIBUTES.contains(attribute.name().toLowerCase(Locale.ROOT)))
            .map(StructuredTemplateParser.TemplateAttribute::value)
            .map(String::trim)
            .findFirst();
    }

    private String unwrapFragmentExpression(String expression) {
        String normalized = expression.trim();
        if (normalized.startsWith("~{") && normalized.endsWith("}")) {
            return normalized.substring(2, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private String unquote(String value) {
        if (value.length() < 2) {
            return value;
        }
        if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    public record DependencyComponent(String templatePath, String fragmentName, String encodedTemplatePath) {
        public String key() {
            return templatePath + "::" + fragmentName;
        }
    }
}
