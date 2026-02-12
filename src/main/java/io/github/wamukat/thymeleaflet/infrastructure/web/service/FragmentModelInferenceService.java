package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.domain.model.InferredModel;
import io.github.wamukat.thymeleaflet.domain.model.TemplateInferenceSnapshot;
import io.github.wamukat.thymeleaflet.domain.service.ModelValueInferenceService;
import io.github.wamukat.thymeleaflet.domain.service.TemplateModelExpressionAnalyzer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
    private final ModelValueInferenceService valueInferenceService;

    public FragmentModelInferenceService(
        ResourceLoader resourceLoader,
        TemplateModelExpressionAnalyzer expressionAnalyzer,
        ModelValueInferenceService valueInferenceService
    ) {
        this.resourceLoader = resourceLoader;
        this.expressionAnalyzer = expressionAnalyzer;
        this.valueInferenceService = valueInferenceService;
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

        TemplateInferenceSnapshot snapshot = expressionAnalyzer.analyze(html, new HashSet<>(parameterNames));
        InferredModel inferred = buildInferredModel(snapshot);
        for (String referencedTemplatePath : snapshot.referencedTemplatePaths()) {
            if (referencedTemplatePath.equals(templatePath)) {
                continue;
            }
            InferredModel child = inferModelRecursive(referencedTemplatePath, List.of(), visitedTemplatePaths);
            inferred.merge(child);
        }
        return inferred;
    }

    private InferredModel buildInferredModel(TemplateInferenceSnapshot snapshot) {
        InferredModel inferred = new InferredModel();
        for (List<String> path : snapshot.modelPaths()) {
            if (path.isEmpty()) {
                continue;
            }
            String root = path.getFirst();
            Object leafValue = valueInferenceService.inferLeafValue(path.get(path.size() - 1));
            if (snapshot.loopVariablePaths().containsKey(root)) {
                inferred.putLoopPath(snapshot.loopVariablePaths().get(root), path.subList(1, path.size()), leafValue);
                continue;
            }
            inferred.putPath(path, leafValue);
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
