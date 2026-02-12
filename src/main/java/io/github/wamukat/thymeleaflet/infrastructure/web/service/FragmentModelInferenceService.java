package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.domain.model.InferredModel;
import io.github.wamukat.thymeleaflet.domain.model.TemplateInferenceSnapshot;
import io.github.wamukat.thymeleaflet.domain.service.TemplateModelExpressionAnalyzer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
            Object leafValue = inferLeafValue(path.get(path.size() - 1));
            if (snapshot.loopVariablePaths().containsKey(root)) {
                inferred.putLoopPath(snapshot.loopVariablePaths().get(root), path.subList(1, path.size()), leafValue);
                continue;
            }
            inferred.putPath(path, leafValue);
        }
        return inferred;
    }

    private Object inferLeafValue(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("is")
            || normalized.startsWith("has")
            || normalized.startsWith("can")
            || normalized.startsWith("should")
            || normalized.startsWith("enabled")
            || normalized.startsWith("active")) {
            return false;
        }
        if (normalized.contains("count")
            || normalized.contains("total")
            || normalized.contains("amount")
            || normalized.contains("price")
            || normalized.contains("point")
            || normalized.contains("score")
            || normalized.contains("num")
            || normalized.contains("size")
            || normalized.contains("balance")
            || normalized.contains("age")
            || normalized.contains("rate")
            || normalized.contains("percent")) {
            return 0;
        }
        if (normalized.contains("date") || normalized.contains("time")) {
            return "2026-01-01";
        }
        if (normalized.contains("email")) {
            return "sample@example.com";
        }
        return "Sample " + key;
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
