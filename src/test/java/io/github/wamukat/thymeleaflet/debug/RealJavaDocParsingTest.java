package io.github.wamukat.thymeleaflet.debug;

import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.TypeInformationExtractor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 実際のHTMLファイルからのJavaDoc解析テスト
 */
@SpringBootTest(classes = {io.github.wamukat.thymeleaflet.config.FragmentTestConfiguration.class})
@ActiveProfiles("test")
@Tag("debug")
class RealJavaDocParsingTest {

    @Autowired
    private JavaDocAnalyzer javaDocAnalyzer;
    
    @Autowired
    private TypeInformationExtractor typeInformationExtractor;

    @Test
    void testRealTransactionBadgeJavaDocParsing() throws Exception {
        System.out.println("=== Testing real point-transaction-badge.html JavaDoc parsing ===");
        
        // 実際のHTMLファイルを読み込む
        String htmlPath = "/Users/takuma/workspace/claude-code/mypage/mypage-ui-app/src/main/resources/templates/domain/point/molecules/point-transaction-badge.html";
        String htmlContent = Files.readString(Paths.get(htmlPath));
        
        System.out.println("HTML file size: " + htmlContent.length() + " characters");
        
        // JavaDoc解析を実行
        List<JavaDocAnalyzer.JavaDocInfo> javadocInfos = javaDocAnalyzer.analyzeJavaDocFromHtml(htmlContent);
        
        System.out.println("Found " + javadocInfos.size() + " JavaDoc blocks");
        
        for (int i = 0; i < javadocInfos.size(); i++) {
            JavaDocAnalyzer.JavaDocInfo info = javadocInfos.get(i);
            System.out.println("\n--- JavaDoc Block " + (i + 1) + " ---");
            System.out.println("Description: " + info.getDescription());
            System.out.println("Parameters: " + info.getParameters().size());
            
            for (JavaDocAnalyzer.ParameterInfo param : info.getParameters()) {
                System.out.println("  Parameter: " + param.getName());
                System.out.println("    Type: " + param.getType());
                System.out.println("    Required: " + param.isRequired());
                System.out.println("    Default: " + param.getDefaultValue());
                System.out.println("    Description: " + param.getDescription());
                System.out.println("    Allowed Values: " + param.getAllowedValues());
            }
            
            System.out.println("Examples: " + info.getExamples().size());
            for (JavaDocAnalyzer.ExampleInfo example : info.getExamples()) {
                System.out.println("  Example: " + example.getTemplatePath() + "::" + example.getFragmentName());
                System.out.println("    Arguments: " + example.getArguments());
            }
        }
        
        // 型推論を実行
        System.out.println("\n=== Type Information Extraction ===");
        List<TypeInfo> typeInfos = typeInformationExtractor.extractTypeInformationFromHtml(htmlContent);
        
        System.out.println("Extracted " + typeInfos.size() + " type infos");
        
        for (TypeInfo typeInfo : typeInfos) {
            System.out.println("\n--- Type Info ---");
            System.out.println("Parameter Name: " + typeInfo.getParameterName());
            System.out.println("Java Type Name: " + typeInfo.getJavaTypeName());
            System.out.println("Type Category: " + typeInfo.getTypeCategory());
            System.out.println("Inference Level: " + typeInfo.getInferenceLevel());
            System.out.println("Allowed Values: " + typeInfo.getAllowedValues());
            System.out.println("Description: " + typeInfo.getDescription());
            
            // TransactionTypeのENUM判定を確認
            if ("transactionType".equals(typeInfo.getParameterName())) {
                if (typeInfo.getTypeCategory() == TypeInfo.TypeCategory.ENUM) {
                    System.out.println("✅ TransactionType correctly identified as ENUM");
                    if (typeInfo.getAllowedValues().contains("EARN") && typeInfo.getAllowedValues().contains("USE")) {
                        System.out.println("✅ ENUM values (EARN, USE) correctly inferred");
                    } else {
                        System.out.println("❌ ENUM values missing or incorrect: " + typeInfo.getAllowedValues());
                    }
                } else {
                    System.out.println("❌ TransactionType NOT identified as ENUM (category: " + typeInfo.getTypeCategory() + ")");
                }
            }
        }
    }
    
    @Test
    void testJavaDocRegexPatterns() throws Exception {
        System.out.println("=== Testing JavaDoc regex patterns ===");
        
        String htmlPath = "/Users/takuma/workspace/claude-code/mypage/mypage-ui-app/src/main/resources/templates/domain/point/molecules/point-transaction-badge.html";
        String htmlContent = Files.readString(Paths.get(htmlPath));
        
        // JavaDocコメントブロックの検出確認
        boolean hasJavaDocComment = htmlContent.contains("/**") && htmlContent.contains("*/");
        System.out.println("Contains JavaDoc comment markers: " + hasJavaDocComment);
        
        // @paramパターンの検出確認
        boolean hasParamTag = htmlContent.contains("@param");
        System.out.println("Contains @param tags: " + hasParamTag);
        
        // TransactionTypeパターンの検出確認
        boolean hasTransactionType = htmlContent.contains("TransactionType");
        System.out.println("Contains TransactionType: " + hasTransactionType);
        
        // HTML構造の確認
        boolean hasThFragment = htmlContent.contains("th:fragment");
        System.out.println("Contains th:fragment: " + hasThFragment);
        
        if (hasThFragment) {
            // th:fragmentの名前を抽出
            int fragmentStart = htmlContent.indexOf("th:fragment=\"");
            if (fragmentStart != -1) {
                int fragmentEnd = htmlContent.indexOf("\"", fragmentStart + 13);
                String fragmentName = htmlContent.substring(fragmentStart + 13, fragmentEnd);
                System.out.println("Fragment name: " + fragmentName);
            }
        }
    }
}
