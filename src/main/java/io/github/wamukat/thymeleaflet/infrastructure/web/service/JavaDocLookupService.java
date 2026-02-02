package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JavaDoc情報取得専用サービス
 *
 * 責務: テンプレートからJavaDoc情報を取得する処理を一箇所に集約
 */
@Component
public class JavaDocLookupService {

    private static final Logger logger = LoggerFactory.getLogger(JavaDocLookupService.class);

    private final JavaDocContentService javaDocContentService;

    public JavaDocLookupService(JavaDocContentService javaDocContentService) {
        this.javaDocContentService = javaDocContentService;
    }

    public JavaDocAnalyzer.JavaDocInfo findJavaDocInfo(String templatePath, String fragmentName) {
        List<JavaDocAnalyzer.JavaDocInfo> javadocInfos = javaDocContentService.loadJavaDocInfos(templatePath);
        if (javadocInfos.isEmpty()) {
            return null;
        }

        try {
            return javadocInfos.stream()
                .filter(doc -> {
                    boolean matchesDescription = doc.getDescription() != null
                        && doc.getDescription().toLowerCase().contains(fragmentName.toLowerCase());
                    boolean matchesExample = doc.getExamples().stream()
                        .anyMatch(ex -> ex.getFragmentName().equals(fragmentName));
                    return matchesDescription || matchesExample;
                })
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            logger.warn("Failed to read JavaDoc info for {}::{}: {}", templatePath, fragmentName, e.getMessage());
            return null;
        }
    }

    public boolean hasJavaDoc(String templatePath, String fragmentName) {
        return findJavaDocInfo(templatePath, fragmentName) != null;
    }
}
