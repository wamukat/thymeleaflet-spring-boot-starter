package io.github.wamukat.thymeleaflet.domain.service;

import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;

import java.util.List;

/**
 * ドキュメント分析サービス - Domain層インターフェース
 * 
 * Clean Architecture準拠:
 * - ドメイン層が定義するインターフェース
 * - Infrastructure層で実装される
 * - 依存関係逆転原理 (DIP) の適用
 */
public interface DocumentationAnalysisService {
    
    /**
     * テンプレートパスから型情報を抽出
     * 
     * @param templatePath テンプレートパス
     * @return 型情報リスト
     */
    List<TypeInfo> extractTypeInformation(String templatePath);
}