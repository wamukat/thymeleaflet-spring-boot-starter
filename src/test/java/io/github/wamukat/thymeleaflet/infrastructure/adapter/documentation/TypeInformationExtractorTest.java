package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TypeInformationExtractor用ユニットテスト
 * Phase 5.1: Infrastructure層型情報抽出機能のテスト
 */
@DisplayName("TypeInformationExtractor Tests")
class TypeInformationExtractorTest {

    private TypeInformationExtractor extractor;

    @Mock
    private JavaDocAnalyzer mockJavaDocAnalyzer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        extractor = new TypeInformationExtractor(mockJavaDocAnalyzer);
    }

    @Test
    @DisplayName("HTMLコンテンツから型情報を正常に抽出できる")
    void shouldExtractTypeInformationFromHtml() {
        // Given
        String htmlContent = "<!-- JavaDoc content -->";
        
        JavaDocAnalyzer.ParameterInfo paramInfo1 = JavaDocAnalyzer.ParameterInfo.of(
            "text", "String", true, Optional.empty(), Optional.of("Button display text"), Collections.emptyList()
        );
        JavaDocAnalyzer.ParameterInfo paramInfo2 = JavaDocAnalyzer.ParameterInfo.of(
            "variant", "ButtonType", false, "primary", "Button style variant", Arrays.asList("primary", "secondary")
        );
        
        JavaDocAnalyzer.JavaDocInfo mockJavaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "Button component",
            Arrays.asList(paramInfo1, paramInfo2),
            Collections.emptyList(),
            java.util.Optional.empty()
        );

        when(mockJavaDocAnalyzer.analyzeJavaDocFromHtml(any(String.class)))
            .thenReturn(Arrays.asList(mockJavaDocInfo));

        // When
        List<TypeInfo> result = extractor.extractTypeInformationFromHtml(htmlContent);

        // Then
        assertThat(result).hasSize(2);
        
        TypeInfo textTypeInfo = result.get(0);
        assertThat(textTypeInfo.getParameterName()).isEqualTo("text");
        assertThat(textTypeInfo.getJavaTypeName()).isEqualTo("String");
        assertThat(textTypeInfo.getTypeCategory()).isEqualTo(TypeInfo.TypeCategory.PRIMITIVE);
        assertThat(textTypeInfo.getInferenceLevel()).isEqualTo(TypeInfo.InferenceLevel.EXPLICIT);
        
        TypeInfo variantTypeInfo = result.get(1);
        assertThat(variantTypeInfo.getParameterName()).isEqualTo("variant");
        assertThat(variantTypeInfo.getJavaTypeName()).isEqualTo("ButtonType");
        assertThat(variantTypeInfo.getTypeCategory()).isEqualTo(TypeInfo.TypeCategory.ENUM);
        assertThat(variantTypeInfo.getAllowedValues()).containsExactly("primary", "secondary");
    }

    @Test
    @DisplayName("プリミティブ型の判定が正常に動作する")
    void shouldCorrectlyDeterminePrimitiveTypes() {
        // Given
        String htmlContent = "test";
        
        JavaDocAnalyzer.ParameterInfo stringParam = JavaDocAnalyzer.ParameterInfo.of(
            "text", "String", true, Optional.empty(), Optional.of("String parameter"), Collections.emptyList()
        );
        JavaDocAnalyzer.ParameterInfo booleanParam = JavaDocAnalyzer.ParameterInfo.of(
            "enabled", "Boolean", false, "true", "Boolean parameter", Collections.emptyList()
        );
        JavaDocAnalyzer.ParameterInfo intParam = JavaDocAnalyzer.ParameterInfo.of(
            "count", "Integer", false, "0", "Integer parameter", Collections.emptyList()
        );
        
        JavaDocAnalyzer.JavaDocInfo mockJavaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "Test component",
            Arrays.asList(stringParam, booleanParam, intParam),
            Collections.emptyList(),
            java.util.Optional.empty()
        );

        when(mockJavaDocAnalyzer.analyzeJavaDocFromHtml(any(String.class)))
            .thenReturn(Arrays.asList(mockJavaDocInfo));

        // When
        List<TypeInfo> result = extractor.extractTypeInformationFromHtml(htmlContent);

        // Then
        assertThat(result).hasSize(3);
        
        // 全てPRIMITIVE型として判定されること
        result.forEach(typeInfo -> {
            assertThat(typeInfo.getTypeCategory()).isEqualTo(TypeInfo.TypeCategory.PRIMITIVE);
            assertThat(typeInfo.getInferenceLevel()).isEqualTo(TypeInfo.InferenceLevel.EXPLICIT);
        });
    }

    @Test
    @DisplayName("Enum型の判定が正常に動作する")
    void shouldCorrectlyDetermineEnumTypes() {
        // Given
        String htmlContent = "test";
        
        JavaDocAnalyzer.ParameterInfo enumParam1 = JavaDocAnalyzer.ParameterInfo.of(
            "transactionType", "TransactionType", true, Optional.empty(), Optional.of("Transaction type"), Arrays.asList("EARN", "USE")
        );
        JavaDocAnalyzer.ParameterInfo enumParam2 = JavaDocAnalyzer.ParameterInfo.of(
            "status", "Status", false, "ACTIVE", "Status value", Arrays.asList("ACTIVE", "INACTIVE")
        );
        
        JavaDocAnalyzer.JavaDocInfo mockJavaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "Test component with enums",
            Arrays.asList(enumParam1, enumParam2),
            Collections.emptyList(),
            java.util.Optional.empty()
        );

        when(mockJavaDocAnalyzer.analyzeJavaDocFromHtml(any(String.class)))
            .thenReturn(Arrays.asList(mockJavaDocInfo));

        // When
        List<TypeInfo> result = extractor.extractTypeInformationFromHtml(htmlContent);

        // Then
        assertThat(result).hasSize(2);
        
        result.forEach(typeInfo -> {
            assertThat(typeInfo.getTypeCategory()).isEqualTo(TypeInfo.TypeCategory.ENUM);
            assertThat(typeInfo.getAllowedValues()).isNotEmpty();
        });
        
        // TransactionType特別処理の確認
        TypeInfo transactionTypeInfo = result.stream()
            .filter(t -> t.getParameterName().equals("transactionType"))
            .findFirst().orElseThrow();
        assertThat(transactionTypeInfo.getAllowedValues()).containsExactlyInAnyOrder("EARN", "USE");
    }

    @Test
    @DisplayName("Collection型の判定が正常に動作する")
    void shouldCorrectlyDetermineCollectionTypes() {
        // Given
        String htmlContent = "test";
        
        JavaDocAnalyzer.ParameterInfo listParam = JavaDocAnalyzer.ParameterInfo.of(
            "items", "List<String>", false, Optional.empty(), Optional.of("List of items"), Collections.emptyList()
        );
        JavaDocAnalyzer.ParameterInfo setParam = JavaDocAnalyzer.ParameterInfo.of(
            "tags", "Set<Tag>", false, Optional.empty(), Optional.of("Set of tags"), Collections.emptyList()
        );
        
        JavaDocAnalyzer.JavaDocInfo mockJavaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "Test component with collections",
            Arrays.asList(listParam, setParam),
            Collections.emptyList(),
            java.util.Optional.empty()
        );

        when(mockJavaDocAnalyzer.analyzeJavaDocFromHtml(any(String.class)))
            .thenReturn(Arrays.asList(mockJavaDocInfo));

        // When
        List<TypeInfo> result = extractor.extractTypeInformationFromHtml(htmlContent);

        // Then
        assertThat(result).hasSize(2);
        
        result.forEach(typeInfo -> {
            assertThat(typeInfo.getTypeCategory()).isEqualTo(TypeInfo.TypeCategory.COLLECTION);
        });
    }

    @Test
    @DisplayName("名前による型情報検索が正常に動作する")
    void shouldFindTypeInfoByName() {
        // Given
        TypeInfo typeInfo1 = new TypeInfo.Builder()
            .parameterName("text")
            .javaTypeName("String")
            .typeCategory(TypeInfo.TypeCategory.PRIMITIVE)
            .inferenceLevel(TypeInfo.InferenceLevel.EXPLICIT)
            .build();
            
        TypeInfo typeInfo2 = new TypeInfo.Builder()
            .parameterName("count")
            .javaTypeName("Integer")
            .typeCategory(TypeInfo.TypeCategory.PRIMITIVE)
            .inferenceLevel(TypeInfo.InferenceLevel.EXPLICIT)
            .build();
            
        List<TypeInfo> typeInfos = Arrays.asList(typeInfo1, typeInfo2);

        // When
        var found = extractor.findTypeInfoByName(typeInfos, "text");
        var notFound = extractor.findTypeInfoByName(typeInfos, "nonexistent");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getParameterName()).isEqualTo("text");
        assertThat(found.get().getJavaTypeName()).isEqualTo("String");
        
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("空のHTMLコンテンツでも正常に処理される")
    void shouldHandleEmptyHtmlContent() {
        // Given
        when(mockJavaDocAnalyzer.analyzeJavaDocFromHtml(any(String.class)))
            .thenReturn(Collections.emptyList());

        // When
        List<TypeInfo> result = extractor.extractTypeInformationFromHtml("");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("解析例外が発生してもログを出力して空のリストを返す")
    void shouldHandleAnalysisExceptions() {
        // Given
        when(mockJavaDocAnalyzer.analyzeJavaDocFromHtml(any(String.class)))
            .thenThrow(new RuntimeException("Analysis failed"));

        // When
        List<TypeInfo> result = extractor.extractTypeInformationFromHtml("invalid html");

        // Then
        assertThat(result).isEmpty();
    }
}
