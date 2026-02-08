package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResourcePathValidator;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaDoc解析用のテンプレート読み込みと解析を集約するサービス。
 */
@Component
public class JavaDocContentService {

    private static final Logger logger = LoggerFactory.getLogger(JavaDocContentService.class);

    private final JavaDocAnalyzer javaDocAnalyzer;
    private final StorybookProperties storybookProperties;
    private final ResourcePathValidator resourcePathValidator;
    private final Map<String, List<JavaDocAnalyzer.JavaDocInfo>> javaDocCache = new ConcurrentHashMap<>();
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public JavaDocContentService(JavaDocAnalyzer javaDocAnalyzer,
                                 StorybookProperties storybookProperties,
                                 ResourcePathValidator resourcePathValidator) {
        this.javaDocAnalyzer = javaDocAnalyzer;
        this.storybookProperties = storybookProperties;
        this.resourcePathValidator = resourcePathValidator;
    }

    public Optional<String> loadTemplateContent(String templatePath) {
        if (storybookProperties.getCache().isEnabled()) {
            Optional<String> cached = Optional.ofNullable(templateCache.get(templatePath));
            if (cached.isPresent()) {
                return cached;
            }
        }
        try {
            Resource resource = resourcePathValidator.findTemplate(
                templatePath,
                storybookProperties.getResources().getTemplatePaths()
            );

            if (!resource.exists()) {
                logger.debug("Template not found: {}", templatePath);
                return Optional.empty();
            }

            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (storybookProperties.getCache().isEnabled()) {
                templateCache.put(templatePath, content);
            }
            return Optional.of(content);
        } catch (Exception e) {
            logger.warn("Failed to read template content for {}: {}", templatePath, e.getMessage());
            return Optional.empty();
        }
    }

    public List<JavaDocAnalyzer.JavaDocInfo> loadJavaDocInfos(String templatePath) {
        if (storybookProperties.getCache().isEnabled()) {
            Optional<List<JavaDocAnalyzer.JavaDocInfo>> cached = Optional.ofNullable(javaDocCache.get(templatePath));
            if (cached.isPresent()) {
                return cached.orElseThrow();
            }
        }
        Optional<String> htmlContent = loadTemplateContent(templatePath);
        if (htmlContent.isEmpty() || htmlContent.get().isBlank()) {
            return Collections.emptyList();
        }

        try {
            List<JavaDocAnalyzer.JavaDocInfo> docs = javaDocAnalyzer.analyzeJavaDocFromHtml(htmlContent.get());
            if (storybookProperties.getCache().isEnabled()) {
                javaDocCache.put(templatePath, docs);
            }
            return docs;
        } catch (Exception e) {
            logger.warn("Failed to analyze JavaDoc for {}: {}", templatePath, e.getMessage());
            return Collections.emptyList();
        }
    }
}
