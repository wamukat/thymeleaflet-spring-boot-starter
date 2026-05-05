package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.domain.model.InferredModel;
import io.github.wamukat.thymeleaflet.domain.model.ModelPath;
import io.github.wamukat.thymeleaflet.domain.model.TemplateInference;
import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;
import io.github.wamukat.thymeleaflet.domain.service.TemplateModelExpressionAnalyzer;
import io.github.wamukat.thymeleaflet.domain.service.TopLevelSyntaxScanner;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentSignatureParser;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * テンプレート内の式からモデル値を推定するサービス。
 *
 * 推定値は Custom story の初期モデル候補として利用する。
 */
@Component
public class FragmentModelInferenceService {

    private final ResourceLoader resourceLoader;
    private final TemplateModelExpressionAnalyzer expressionAnalyzer;
    private final StructuredTemplateParser templateParser = new StructuredTemplateParser();
    private final FragmentSignatureParser fragmentSignatureParser = new FragmentSignatureParser();
    private final TopLevelSyntaxScanner topLevelSyntaxScanner = new TopLevelSyntaxScanner();

    public FragmentModelInferenceService(
        ResourceLoader resourceLoader,
        TemplateModelExpressionAnalyzer expressionAnalyzer
    ) {
        this.resourceLoader = resourceLoader;
        this.expressionAnalyzer = expressionAnalyzer;
    }

    public Map<String, Object> inferModel(String templatePath, String fragmentName, List<String> parameterNames) {
        InferredModel inferred = inferModelRecursive(
            templatePath,
            parameterNames,
            new HashSet<>()
        );
        return inferred.toMap();
    }

    public Map<String, Object> inferMethodReturnCandidates(
        String templatePath,
        String fragmentName,
        @Nullable List<String> parameterNames
    ) {
        InferredModel inferred = inferMethodReturnCandidatesRecursive(
            templatePath,
            parameterNames == null ? List.of() : parameterNames,
            new HashSet<>()
        );
        return inferred.toMap();
    }

    private InferredModel inferModelRecursive(
        String templatePath,
        List<String> parameterNames,
        Set<String> visitedTemplatePaths
    ) {
        if (!visitedTemplatePaths.add(templatePath)) {
            return new InferredModel();
        }
        String html = readTemplateSource(templatePath);
        if (html.isEmpty()) {
            return new InferredModel();
        }

        TemplateInference inference = expressionAnalyzer.analyze(html, new HashSet<>(parameterNames), templatePath);
        InferredModel inferred = inference.toInferredModel();
        for (TemplateInference.ReferencedFragment reference : inference.referencedFragments()) {
            if (!reference.requiresChildModelRecursion()) {
                continue;
            }
            String referencedTemplatePath = reference.templatePath();
            if (referencedTemplatePath.equals(templatePath)) {
                continue;
            }
            InferredModel child = inferModelRecursive(
                referencedTemplatePath,
                mappedChildParameterNames(reference),
                visitedTemplatePaths
            );
            inferred.merge(child);
        }
        return inferred;
    }

    private InferredModel inferMethodReturnCandidatesRecursive(
        String templatePath,
        List<String> parameterNames,
        Set<String> visitedTemplatePaths
    ) {
        if (!visitedTemplatePaths.add(templatePath)) {
            return new InferredModel();
        }
        String html = readTemplateSource(templatePath);
        if (html.isEmpty()) {
            return new InferredModel();
        }

        TemplateInference inference = expressionAnalyzer.analyze(html, new HashSet<>(parameterNames), templatePath);
        InferredModel inferred = new InferredModel();
        for (ModelPath methodPath : inference.noArgMethodPaths()) {
            if (methodPath.isEmpty()) {
                continue;
            }
            ModelPath loopPath = inference.loopVariablePaths().get(methodPath.root());
            if (loopPath != null) {
                inferred.putLoopPath(loopPath.segments(), methodPath.subPathWithoutRoot(), methodPath.inferSampleValue());
                continue;
            }
            inferred.putPath(methodPath.segments(), methodPath.inferSampleValue());
        }

        for (TemplateInference.ReferencedFragment reference : inference.referencedFragments()) {
            if (!reference.requiresChildModelRecursion()) {
                continue;
            }
            String referencedTemplatePath = reference.templatePath();
            if (referencedTemplatePath.equals(templatePath)) {
                continue;
            }
            InferredModel child = inferMethodReturnCandidatesRecursive(
                referencedTemplatePath,
                mappedChildParameterNames(reference),
                visitedTemplatePaths
            );
            inferred.merge(child);
        }

        return inferred;
    }

    private List<String> mappedChildParameterNames(TemplateInference.ReferencedFragment reference) {
        if (!reference.hasArgumentList() || reference.arguments().isEmpty()) {
            return List.of();
        }
        List<String> argumentNames = namedArgumentNames(reference.arguments());
        if (argumentNames.isEmpty()) {
            return List.of();
        }
        List<String> declarationParameters = fragmentParameterNames(reference.templatePath(), reference.fragmentName());
        if (!declarationParameters.containsAll(argumentNames)) {
            return List.of();
        }
        return argumentNames;
    }

    private List<String> namedArgumentNames(List<String> arguments) {
        List<String> argumentNames = new ArrayList<>();
        for (String argument : arguments) {
            Optional<String> name = namedArgumentName(argument);
            if (name.isEmpty()) {
                return List.of();
            }
            argumentNames.add(name.orElseThrow());
        }
        return argumentNames;
    }

    private Optional<String> namedArgumentName(String argument) {
        var assignIndex = topLevelSyntaxScanner.findFirst(argument, "=");
        if (assignIndex.isEmpty() || assignIndex.orElseThrow() <= 0) {
            return Optional.empty();
        }
        String candidate = argument.substring(0, assignIndex.orElseThrow()).trim();
        if (!isIdentifier(candidate)) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }

    private boolean isIdentifier(String candidate) {
        if (candidate.isBlank() || !Character.isLetterOrDigit(candidate.charAt(0))) {
            return false;
        }
        for (int index = 1; index < candidate.length(); index++) {
            char current = candidate.charAt(index);
            if (!Character.isLetterOrDigit(current) && current != '_' && current != '-') {
                return false;
            }
        }
        return true;
    }

    private List<String> fragmentParameterNames(String templatePath, String fragmentName) {
        String html = readTemplateSource(templatePath);
        if (html.isEmpty()) {
            return List.of();
        }
        StructuredTemplateParser.ParsedTemplate parsedTemplate = templateParser.parse(html);
        for (StructuredTemplateParser.TemplateElement element : parsedTemplate.elements()) {
            Optional<List<String>> parameters = parseFragmentParameters(element, fragmentName);
            if (parameters.isPresent()) {
                return parameters.orElseThrow();
            }
        }
        return List.of();
    }

    private Optional<List<String>> parseFragmentParameters(
        StructuredTemplateParser.TemplateElement element,
        String fragmentName
    ) {
        Optional<String> definition = element.attributeValue("th:fragment")
            .or(() -> element.attributeValue("data-th-fragment"));
        if (definition.isEmpty()) {
            return Optional.empty();
        }
        FragmentSignatureParser.ParseResult result = fragmentSignatureParser.parse(definition.orElseThrow());
        if (result instanceof FragmentSignatureParser.ParseSuccess success && success.fragmentName().equals(fragmentName)) {
            return Optional.of(success.parameters());
        }
        return Optional.empty();
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
