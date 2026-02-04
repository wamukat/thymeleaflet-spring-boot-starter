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
        
        // 実際のHTMLファイルを読み込む
        String htmlPath = "/Users/takuma/workspace/claude-code/mypage/mypage-ui-app/src/main/resources/templates/domain/point/molecules/point-transaction-badge.html";
        String htmlContent = Files.readString(Paths.get(htmlPath));
        
        
        // JavaDoc解析を実行
        List<JavaDocAnalyzer.JavaDocInfo> javadocInfos = javaDocAnalyzer.analyzeJavaDocFromHtml(htmlContent);
        
        
        for (int i = 0; i < javadocInfos.size(); i++) {
            JavaDocAnalyzer.JavaDocInfo info = javadocInfos.get(i);
            
            for (JavaDocAnalyzer.ParameterInfo param : info.getParameters()) {
            }
            
            for (JavaDocAnalyzer.ExampleInfo example : info.getExamples()) {
            }
        }
        
        // 型推論を実行
        List<TypeInfo> typeInfos = typeInformationExtractor.extractTypeInformationFromHtml(htmlContent);
        
        
        for (TypeInfo typeInfo : typeInfos) {
            
            // TransactionTypeのENUM判定を確認
            if ("transactionType".equals(typeInfo.getParameterName())) {
                if (typeInfo.getTypeCategory() == TypeInfo.TypeCategory.ENUM) {
                    if (typeInfo.getAllowedValues().contains("EARN") && typeInfo.getAllowedValues().contains("USE")) {
                    } else {
                    }
                } else {
                }
            }
        }
    }
    
    @Test
    void testJavaDocRegexPatterns() throws Exception {
        
        String htmlPath = "/Users/takuma/workspace/claude-code/mypage/mypage-ui-app/src/main/resources/templates/domain/point/molecules/point-transaction-badge.html";
        String htmlContent = Files.readString(Paths.get(htmlPath));
        
        // JavaDocコメントブロックの検出確認
        boolean hasJavaDocComment = htmlContent.contains("/**") && htmlContent.contains("*/");
        
        // @paramパターンの検出確認
        boolean hasParamTag = htmlContent.contains("@param");
        
        // TransactionTypeパターンの検出確認
        boolean hasTransactionType = htmlContent.contains("TransactionType");
        
        // HTML構造の確認
        boolean hasThFragment = htmlContent.contains("th:fragment");
        
        if (hasThFragment) {
            // th:fragmentの名前を抽出
            int fragmentStart = htmlContent.indexOf("th:fragment=\"");
            if (fragmentStart != -1) {
                int fragmentEnd = htmlContent.indexOf("\"", fragmentStart + 13);
                String fragmentName = htmlContent.substring(fragmentStart + 13, fragmentEnd);
            }
        }
    }
}
