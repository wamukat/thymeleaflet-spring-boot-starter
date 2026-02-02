package io.github.wamukat.thymeleaflet.infrastructure.adapter.service;

import io.github.wamukat.thymeleaflet.application.port.outbound.DocumentationAnalysisPort;
import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocContentService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.TypeInformationExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
    
    public DocumentationAnalysisAdapter(TypeInformationExtractor typeInformationExtractor,
                                        JavaDocContentService javaDocContentService) {
        this.typeInformationExtractor = typeInformationExtractor;
        this.javaDocContentService = javaDocContentService;
    }
    
    @Override
    public List<TypeInfo> extractTypeInformation(String templatePath) {
        try {
            String htmlContent = javaDocContentService.loadTemplateContent(templatePath);
            if (htmlContent == null || htmlContent.isBlank()) {
                return new ArrayList<>();
            }
            return typeInformationExtractor.extractTypeInformationFromHtml(htmlContent);
            
        } catch (Exception e) {
            logger.error("Failed to extract type information from {}: {}", templatePath, e.getMessage());
            return new ArrayList<>();
        }
    }

}
