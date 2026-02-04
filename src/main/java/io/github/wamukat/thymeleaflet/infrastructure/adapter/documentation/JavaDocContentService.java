package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResourcePathValidator;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * JavaDoc解析用のテンプレート読み込みと解析を集約するサービス。
 */
@Component
public class JavaDocContentService {

    private static final Logger logger = LoggerFactory.getLogger(JavaDocContentService.class);

    private final JavaDocAnalyzer javaDocAnalyzer;
    private final StorybookProperties storybookProperties;
    private final ResourcePathValidator resourcePathValidator;

    public JavaDocContentService(JavaDocAnalyzer javaDocAnalyzer,
                                 StorybookProperties storybookProperties,
                                 ResourcePathValidator resourcePathValidator) {
        this.javaDocAnalyzer = javaDocAnalyzer;
        this.storybookProperties = storybookProperties;
        this.resourcePathValidator = resourcePathValidator;
    }

    public String loadTemplateContent(String templatePath) {
        try {
            Resource resource = resourcePathValidator.findTemplate(
                templatePath,
                storybookProperties.getResources().getTemplatePaths()
            );

            if (!resource.exists()) {
                logger.debug("Template not found: {}", templatePath);
                return null;
            }

            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Failed to read template content for {}: {}", templatePath, e.getMessage());
            return null;
        }
    }

    public List<JavaDocAnalyzer.JavaDocInfo> loadJavaDocInfos(String templatePath) {
        String htmlContent = loadTemplateContent(templatePath);
        if (htmlContent == null || htmlContent.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return javaDocAnalyzer.analyzeJavaDocFromHtml(htmlContent);
        } catch (Exception e) {
            logger.warn("Failed to analyze JavaDoc for {}: {}", templatePath, e.getMessage());
            return Collections.emptyList();
        }
    }
}
