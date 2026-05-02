package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.outbound.FragmentDependencyPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentExpression;
import io.github.wamukat.thymeleaflet.domain.model.SecureTemplatePath;
import io.github.wamukat.thymeleaflet.domain.service.FragmentExpressionParser;
import io.github.wamukat.thymeleaflet.domain.service.FragmentReferenceAttributes;
import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;
import io.github.wamukat.thymeleaflet.infrastructure.cache.ThymeleafletCacheManager;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentSignatureParser;
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

    private final ResolvedStorybookConfig storybookConfig;

    private final ResourcePathValidator resourcePathValidator;

    private final ThymeleafletCacheManager cacheManager;

    private final StructuredTemplateParser templateParser;
    private final FragmentExpressionParser fragmentExpressionParser;
    private final FragmentSignatureParser fragmentSignatureParser;

    @Autowired
    public FragmentDependencyService(
        ResolvedStorybookConfig storybookConfig,
        ResourcePathValidator resourcePathValidator,
        ThymeleafletCacheManager cacheManager
    ) {
        this(
            storybookConfig,
            resourcePathValidator,
            cacheManager,
            new StructuredTemplateParser(),
            new FragmentExpressionParser(),
            new FragmentSignatureParser()
        );
    }

    FragmentDependencyService(
        ResolvedStorybookConfig storybookConfig,
        ResourcePathValidator resourcePathValidator,
        ThymeleafletCacheManager cacheManager,
        StructuredTemplateParser templateParser
    ) {
        this(
            storybookConfig,
            resourcePathValidator,
            cacheManager,
            templateParser,
            new FragmentExpressionParser(),
            new FragmentSignatureParser()
        );
    }

    FragmentDependencyService(
        ResolvedStorybookConfig storybookConfig,
        ResourcePathValidator resourcePathValidator,
        ThymeleafletCacheManager cacheManager,
        StructuredTemplateParser templateParser,
        FragmentExpressionParser fragmentExpressionParser,
        FragmentSignatureParser fragmentSignatureParser
    ) {
        this.storybookConfig = storybookConfig;
        this.resourcePathValidator = resourcePathValidator;
        this.cacheManager = cacheManager;
        this.templateParser = templateParser;
        this.fragmentExpressionParser = fragmentExpressionParser;
        this.fragmentSignatureParser = fragmentSignatureParser;
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
        return fragmentExpressionParser.parse(expression)
            .map(this::toDependencyComponent);
    }

    private DependencyComponent toDependencyComponent(FragmentExpression expression) {
        String encodedPath = SecureTemplatePath.createUnsafe(expression.templatePath().replace("/", ".")).forUrl();
        return new DependencyComponent(expression.templatePath(), expression.fragmentName(), encodedPath);
    }

    private List<String> extractDependencyExpressions(List<StructuredTemplateParser.TemplateElement> elements) {
        List<String> expressions = new ArrayList<>();
        for (StructuredTemplateParser.TemplateElement element : elements) {
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                if (!attribute.hasValue() || !FragmentReferenceAttributes.isReferenceAttribute(attribute.name())) {
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
        return template.elements().stream()
            .filter(element -> matchesFragmentName(element, fragmentName))
            .findFirst()
            .map(template::subtree)
            .orElse(List.of());
    }

    private boolean matchesFragmentName(StructuredTemplateParser.TemplateElement element, String fragmentName) {
        return fragmentDefinition(element)
            .flatMap(this::parseFragmentName)
            .filter(fragmentName::equals)
            .isPresent();
    }

    private Optional<String> parseFragmentName(String definition) {
        FragmentSignatureParser.ParseResult result = fragmentSignatureParser.parse(definition);
        if (result instanceof FragmentSignatureParser.ParseSuccess success) {
            return Optional.of(success.fragmentName());
        }
        return Optional.empty();
    }

    private Optional<String> fragmentDefinition(StructuredTemplateParser.TemplateElement element) {
        return element.attributes().stream()
            .filter(StructuredTemplateParser.TemplateAttribute::hasValue)
            .filter(attribute -> FRAGMENT_ATTRIBUTES.contains(attribute.name().toLowerCase(Locale.ROOT)))
            .map(StructuredTemplateParser.TemplateAttribute::value)
            .map(String::trim)
            .findFirst();
    }

    public record DependencyComponent(String templatePath, String fragmentName, String encodedTemplatePath) {
        public String key() {
            return templatePath + "::" + fragmentName;
        }
    }
}
