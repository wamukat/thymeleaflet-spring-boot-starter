package io.github.wamukat.thymeleaflet.application.port.outbound;

import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;

import java.util.List;

/**
 * ドキュメント解析ポート
 * 
 * Clean Architectureの依存関係逆転原則に従い、
 * ドメイン層がInfrastructure層の実装に依存しないよう抽象化
 */
public interface DocumentationAnalysisPort {
    
    /**
     * テンプレートから型情報を抽出
     */
    List<TypeInfo> extractTypeInformation(String templatePath);

}