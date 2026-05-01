package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.outbound.JavaDocLookupPort;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * JavaDoc情報取得専用サービス
 *
 * 責務: テンプレートからJavaDoc情報を取得する処理を一箇所に集約
 */
@Component
public class JavaDocLookupService implements JavaDocLookupPort {

    private static final Logger logger = LoggerFactory.getLogger(JavaDocLookupService.class);

    private final JavaDocContentService javaDocContentService;

    public JavaDocLookupService(JavaDocContentService javaDocContentService) {
        this.javaDocContentService = javaDocContentService;
    }

    public Optional<JavaDocAnalyzer.JavaDocInfo> findJavaDocInfo(String templatePath, String fragmentName) {
        List<JavaDocAnalyzer.JavaDocInfo> javadocInfos = javaDocContentService.loadJavaDocInfos(templatePath);
        if (javadocInfos.isEmpty()) {
            return Optional.empty();
        }

        try {
            return javadocInfos.stream()
                .filter(doc -> matchesFragmentTag(doc, fragmentName)
                    || matchesExample(doc, fragmentName)
                    || matchesDescription(doc, fragmentName))
                .findFirst();
        } catch (Exception e) {
            logger.warn("Failed to read JavaDoc info for {}::{}: {}", templatePath, fragmentName, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean matchesFragmentTag(JavaDocAnalyzer.JavaDocInfo doc, String fragmentName) {
        return doc.getFragmentNameOptional()
            .filter(fragmentName::equals)
            .isPresent();
    }

    private boolean matchesExample(JavaDocAnalyzer.JavaDocInfo doc, String fragmentName) {
        return doc.getExamples().stream()
            .anyMatch(ex -> ex.getFragmentName().equals(fragmentName));
    }

    private boolean matchesDescription(JavaDocAnalyzer.JavaDocInfo doc, String fragmentName) {
        return doc.getDescription().toLowerCase(Locale.ROOT).contains(fragmentName.toLowerCase(Locale.ROOT));
    }

    @Override
    public Optional<Object> findJavaDocInfoForView(String templatePath, String fragmentName) {
        return findJavaDocInfo(templatePath, fragmentName).map(doc -> (Object) doc);
    }

    @Override
    public boolean hasJavaDoc(String templatePath, String fragmentName) {
        return findJavaDocInfo(templatePath, fragmentName).isPresent();
    }
}
