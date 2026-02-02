package io.github.wamukat.thymeleaflet.domain.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * フラグメント関連のドメインサービス
 * 
 * FragmentDiscoveryServiceから移設したドメインロジックを集約
 */
@Service
public class FragmentDomainService {

    /**
     * データ依存フラグメントかどうかを判定
     * 
     * @param fragmentName フラグメント名
     * @return データ依存の場合true
     */
    public boolean isDataDependentFragment(String fragmentName) {
        List<String> dataDependentKeywords = Arrays.asList(
            "detail", "info", "profile", "history", "balance", "summary"
        );
        
        return dataDependentKeywords.stream()
                .anyMatch(keyword -> fragmentName.toLowerCase().contains(keyword));
    }

    /**
     * フラグメントの種類を判定
     * 
     * @param templatePath テンプレートパス
     * @param fragmentName フラグメント名
     * @param parameters パラメータリスト
     * @return フラグメント種類
     */
    public FragmentType determineFragmentType(String templatePath, String fragmentName, List<String> parameters) {
        // パラメータなしで共通コンポーネントの場合
        if (parameters.isEmpty() && templatePath.contains("shared/")) {
            return FragmentType.SIMPLE;
        }
        
        // パラメータ付きの場合
        if (!parameters.isEmpty()) {
            return FragmentType.PARAMETERIZED;
        }
        
        // ドメイン固有でデータに依存する場合
        if (templatePath.contains("domain/") || isDataDependentFragment(fragmentName)) {
            return FragmentType.DATA_DEPENDENT;
        }
        
        return FragmentType.SIMPLE;
    }

    /**
     * フラグメント種類列挙型
     */
    public enum FragmentType {
        SIMPLE,          // パラメータなし・ただ置けば使える
        PARAMETERIZED,   // パラメータ付き
        DATA_DEPENDENT   // データ依存・API応答が必要
    }
}