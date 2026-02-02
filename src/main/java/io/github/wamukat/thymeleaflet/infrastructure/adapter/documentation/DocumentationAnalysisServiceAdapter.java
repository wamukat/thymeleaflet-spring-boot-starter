package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import io.github.wamukat.thymeleaflet.application.port.outbound.DocumentationAnalysisPort;
import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;
import io.github.wamukat.thymeleaflet.domain.service.DocumentationAnalysisService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ドキュメント分析サービスアダプター
 * 
 * Clean Architecture準拠:
 * - Domain層のインターフェースをInfrastructure層で実装
 * - Application層のPortを利用してドキュメント分析
 * - 依存関係逆転原理 (DIP) の実装
 */
@Component
public class DocumentationAnalysisServiceAdapter implements DocumentationAnalysisService {
    
    private final DocumentationAnalysisPort documentationAnalysisPort;
    
    public DocumentationAnalysisServiceAdapter(DocumentationAnalysisPort documentationAnalysisPort) {
        this.documentationAnalysisPort = documentationAnalysisPort;
    }
    
    @Override
    public List<TypeInfo> extractTypeInformation(String templatePath) {
        return documentationAnalysisPort.extractTypeInformation(templatePath);
    }
}