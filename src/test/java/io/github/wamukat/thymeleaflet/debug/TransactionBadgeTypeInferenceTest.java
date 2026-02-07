package io.github.wamukat.thymeleaflet.debug;

import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.TypeInformationExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TransactionBadge フラグメントの型推論テスト
 */
@Tag("debug")
class TransactionBadgeTypeInferenceTest {

    private TypeInformationExtractor extractor;

    @Mock
    private JavaDocAnalyzer mockJavaDocAnalyzer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        extractor = new TypeInformationExtractor(mockJavaDocAnalyzer);
    }

    @Test
    void testTransactionTypeEnumInference() {
        
        // Given: 実際のpoint-transaction-badge.htmlのJavaDoc内容を模擬
        JavaDocAnalyzer.ParameterInfo transactionTypeParam = JavaDocAnalyzer.ParameterInfo.of(
            "transactionType", 
            "TransactionType", 
            true, 
            null, 
            "取引タイプ（EARN/USE/その他）", 
            Collections.emptyList()
        );
        
        JavaDocAnalyzer.JavaDocInfo mockJavaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "ポイント取引タイプバッジフラグメント",
            Arrays.asList(transactionTypeParam),
            Collections.emptyList(),
            java.util.Optional.empty()
        );

        when(mockJavaDocAnalyzer.analyzeJavaDocFromHtml(any(String.class)))
            .thenReturn(Arrays.asList(mockJavaDocInfo));

        // When: 型情報を抽出
        List<TypeInfo> result = extractor.extractTypeInformationFromHtml("mock html");

        // Then: TransactionType が ENUM として正しく判定されることを確認
        assertThat(result).hasSize(1);
        
        TypeInfo typeInfo = result.get(0);
        
        // 重要: ENUM型として判定されること
        assertThat(typeInfo.getParameterName()).isEqualTo("transactionType");
        assertThat(typeInfo.getJavaTypeName()).isEqualTo("TransactionType");
        assertThat(typeInfo.getTypeCategory()).isEqualTo(TypeInfo.TypeCategory.ENUM);
        
        // TransactionType特別処理により、EARN/USEが設定されることを確認
        if (typeInfo.getTypeCategory() == TypeInfo.TypeCategory.ENUM) {
            assertThat(typeInfo.getAllowedValues()).containsExactlyInAnyOrder("EARN", "USE");
        }
    }
    
    @Test 
    void testTransactionTypeWithDescriptionPatterns() {
        
        // Given: 説明文にENUM値パターンが含まれる場合
        JavaDocAnalyzer.ParameterInfo transactionTypeParam = JavaDocAnalyzer.ParameterInfo.of(
            "transactionType", 
            "TransactionType", 
            true, 
            null, 
            "取引タイプ（EARN/USE/その他）values: \"EARN\", \"USE\"", 
            Arrays.asList("EARN", "USE")
        );
        
        JavaDocAnalyzer.JavaDocInfo mockJavaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "ポイント取引タイプバッジフラグメント",
            Arrays.asList(transactionTypeParam),
            Collections.emptyList(),
            java.util.Optional.empty()
        );

        when(mockJavaDocAnalyzer.analyzeJavaDocFromHtml(any(String.class)))
            .thenReturn(Arrays.asList(mockJavaDocInfo));

        // When: 型情報を抽出
        List<TypeInfo> result = extractor.extractTypeInformationFromHtml("mock html");

        // Then: ENUM型として判定され、許可値が設定されること
        assertThat(result).hasSize(1);
        
        TypeInfo typeInfo = result.get(0);
        
        assertThat(typeInfo.getTypeCategory()).isEqualTo(TypeInfo.TypeCategory.ENUM);
        assertThat(typeInfo.getAllowedValues()).containsExactlyInAnyOrder("EARN", "USE");
    }
    
    @Test
    void testFailureCaseTypeInference() {
        
        // Given: 型名のみでENUM判定が困難なケース
        JavaDocAnalyzer.ParameterInfo vagueTypeParam = JavaDocAnalyzer.ParameterInfo.of(
            "status", 
            "String", // ENUMではなくStringと誤認される可能性
            false, 
            null, 
            "状態値（ACTIVE/INACTIVE等）", 
            Collections.emptyList()
        );
        
        JavaDocAnalyzer.JavaDocInfo mockJavaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "テストフラグメント",
            Arrays.asList(vagueTypeParam),
            Collections.emptyList(),
            java.util.Optional.empty()
        );

        when(mockJavaDocAnalyzer.analyzeJavaDocFromHtml(any(String.class)))
            .thenReturn(Arrays.asList(mockJavaDocInfo));

        // When: 型情報を抽出
        List<TypeInfo> result = extractor.extractTypeInformationFromHtml("mock html");

        // Then: どのように分類されるかを確認
        assertThat(result).hasSize(1);
        
        TypeInfo typeInfo = result.get(0);
        
        // String型はPRIMITIVEとして分類されるはず
        assertThat(typeInfo.getTypeCategory()).isEqualTo(TypeInfo.TypeCategory.PRIMITIVE);
    }
}
