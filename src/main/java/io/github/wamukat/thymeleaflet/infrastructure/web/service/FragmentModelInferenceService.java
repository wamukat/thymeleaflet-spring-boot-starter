package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.domain.model.InferredModel;
import io.github.wamukat.thymeleaflet.domain.model.TemplateInference;
import io.github.wamukat.thymeleaflet.domain.service.TemplateModelExpressionAnalyzer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

        TemplateInference inference = expressionAnalyzer.analyze(html, new HashSet<>(parameterNames));
        InferredModel inferred = inference.toInferredModel();
        for (Map.Entry<String, Boolean> entry : inference.referencedTemplatePathsWithRecursionFlags().entrySet()) {
            String referencedTemplatePath = entry.getKey();
            boolean requiresRecursion = entry.getValue();
            if (!requiresRecursion) {
                continue;
            }
            if (referencedTemplatePath.equals(templatePath)) {
                continue;
            }
            InferredModel child = inferModelRecursive(referencedTemplatePath, List.of(), visitedTemplatePaths);
            inferred.merge(child);
        }
        return inferred;
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
