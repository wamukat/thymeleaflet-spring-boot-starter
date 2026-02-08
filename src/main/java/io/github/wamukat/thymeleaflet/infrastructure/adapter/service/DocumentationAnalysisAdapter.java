package io.github.wamukat.thymeleaflet.infrastructure.adapter.service;

import io.github.wamukat.thymeleaflet.application.port.outbound.DocumentationAnalysisPort;
import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocContentService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.TypeInformationExtractor;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ドキュメント解析アダプタ
 * 
 * DocumentationAnalysisPortの実装として、JavaDoc解析や型情報抽出を提供
 */
@Component
public class DocumentationAnalysisAdapter implements DocumentationAnalysisPort {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentationAnalysisAdapter.class);
    
    private final TypeInformationExtractor typeInformationExtractor;
    private final JavaDocContentService javaDocContentService;
    private final StorybookProperties storybookProperties;
    private final Map<String, List<TypeInfo>> typeInfoCache = new ConcurrentHashMap<>();
    
    public DocumentationAnalysisAdapter(TypeInformationExtractor typeInformationExtractor,
                                        JavaDocContentService javaDocContentService,
                                        StorybookProperties storybookProperties) {
        this.typeInformationExtractor = typeInformationExtractor;
        this.javaDocContentService = javaDocContentService;
        this.storybookProperties = storybookProperties;
    }
    
    @Override
    public List<TypeInfo> extractTypeInformation(String templatePath) {
        if (storybookProperties.getCache().isEnabled()) {
            Optional<List<TypeInfo>> cached = Optional.ofNullable(typeInfoCache.get(templatePath));
            if (cached.isPresent()) {
                return cached.orElseThrow();
            }
        }
        try {
            var htmlContent = javaDocContentService.loadTemplateContent(templatePath);
            if (htmlContent.isEmpty() || htmlContent.get().isBlank()) {
                return new ArrayList<>();
            }
            List<TypeInfo> result = typeInformationExtractor.extractTypeInformationFromHtml(htmlContent.get());
            if (storybookProperties.getCache().isEnabled()) {
                typeInfoCache.put(templatePath, result);
            }
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to extract type information from {}: {}", templatePath, e.getMessage());
            return new ArrayList<>();
        }
    }

}
