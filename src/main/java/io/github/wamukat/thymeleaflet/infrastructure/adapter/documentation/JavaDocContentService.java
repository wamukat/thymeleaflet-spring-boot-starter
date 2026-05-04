package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResourcePathValidator;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import io.github.wamukat.thymeleaflet.infrastructure.cache.ThymeleafletCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JavaDoc解析用のテンプレート読み込みと解析を集約するサービス。
 */
@Component
public class JavaDocContentService {

    private static final Logger logger = LoggerFactory.getLogger(JavaDocContentService.class);

    private final JavaDocAnalyzer javaDocAnalyzer;
    private final ResolvedStorybookConfig storybookConfig;
    private final ResourcePathValidator resourcePathValidator;
    private final ThymeleafletCacheManager cacheManager;

    public JavaDocContentService(JavaDocAnalyzer javaDocAnalyzer,
                                 ResolvedStorybookConfig storybookConfig,
                                 ResourcePathValidator resourcePathValidator,
                                 ThymeleafletCacheManager cacheManager) {
        this.javaDocAnalyzer = javaDocAnalyzer;
        this.storybookConfig = storybookConfig;
        this.resourcePathValidator = resourcePathValidator;
        this.cacheManager = cacheManager;
    }

    public Optional<String> loadTemplateContent(String templatePath) {
        Optional<String> cached = cacheManager.get("template-content", templatePath);
        if (cached.isPresent()) {
            return cached;
        }
        try {
            Resource resource = resourcePathValidator.findTemplate(
                templatePath,
                storybookConfig.getResources().getTemplatePaths()
            );

            if (!resource.exists()) {
                logger.debug("Template not found: {}", templatePath);
                return Optional.empty();
            }

            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            cacheManager.put("template-content", templatePath, content);
            return Optional.of(content);
        } catch (Exception e) {
            logger.warn("Failed to read template content for {}: {}", templatePath, e.getMessage());
            return Optional.empty();
        }
    }

    public List<JavaDocAnalyzer.JavaDocInfo> loadJavaDocInfos(String templatePath) {
        Optional<List<JavaDocAnalyzer.JavaDocInfo>> cached = cacheManager.get("javadocs", templatePath);
        if (cached.isPresent()) {
            return cached.orElseThrow();
        }
        Optional<String> htmlContent = loadTemplateContent(templatePath);
        if (htmlContent.isEmpty() || htmlContent.get().isBlank()) {
            return Collections.emptyList();
        }

        try {
            List<JavaDocAnalyzer.JavaDocInfo> docs =
                javaDocAnalyzer.analyzeJavaDocFromHtml(htmlContent.get(), templatePath);
            cacheManager.put("javadocs", templatePath, docs);
            return docs;
        } catch (Exception e) {
            logger.warn("Failed to analyze JavaDoc for {}: {}", templatePath, e.getMessage());
            return Collections.emptyList();
        }
    }
}
