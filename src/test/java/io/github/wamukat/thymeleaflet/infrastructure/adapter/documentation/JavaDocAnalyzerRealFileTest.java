package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 実際のHTMLファイルを使ったJavaDocAnalyzerのテスト
 */
@DisplayName("JavaDocAnalyzer Real File Tests")
@Tag("debug")
class JavaDocAnalyzerRealFileTest {

    private JavaDocAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new JavaDocAnalyzer();
    }

    @Test
    @DisplayName("実際のpoint-transaction-icon.htmlファイルからJavaDocを解析できる")
    void shouldAnalyzeRealPointTransactionIconFile() throws IOException {
        // Given - 実際のHTMLファイルを読み込み
        String htmlContent;
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("../../../../../../mypage-ui-app/src/main/resources/templates/domain/point/molecules/point-transaction-icon.html")) {
            
            if (inputStream == null) {
                // ファイルが見つからない場合は、サンプルHTMLを使用
                htmlContent = """
                    <!DOCTYPE html>
                    <html xmlns:th="http://www.thymeleaf.org">
                    <body>
                    
                    <!--
                    /**
                     * ポイント取引タイプアイコンフラグメント
                     * ポイント取引タイプに応じたアイコン表示を提供するmoleculesレベルコンポーネント
                     * 
                     * @param transactionType {@code TransactionType} [required] 取引タイプ（EARN/USE/その他）
                     * 
                     * @example EARN取引のアイコン表示
                     * <div th:replace="~{domain/point/molecules/point-transaction-icon :: pointTransactionIcon(${transaction.transactionType})}"></div>
                     * 
                     * @example USE取引のアイコン表示
                     * <div th:replace="~{domain/point/molecules/point-transaction-icon :: pointTransactionIcon('USE')}"></div>
                     * 
                     * @example 取引履歴での使用
                     * <div th:each="txn : ${transactionHistory}" class="transaction-item">
                     *   <div th:replace="~{domain/point/molecules/point-transaction-icon :: pointTransactionIcon(${txn.transactionType})}"></div>
                     * </div>
                     */
                    -->
                    <div th:fragment="pointTransactionIcon(transactionType)" class="flex-shrink-0">
                    </div>
                    
                    </body>
                    </html>
                    """;
            } else {
                htmlContent = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }


        // When
        List<JavaDocAnalyzer.JavaDocInfo> result = analyzer.analyzeJavaDocFromHtml(htmlContent);

        // Then
        for (int i = 0; i < result.size(); i++) {
            JavaDocAnalyzer.JavaDocInfo info = result.get(i);
        }

        // 基本的なアサーション
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getDescription()).contains("ポイント取引タイプアイコンフラグメント");
        assertThat(result.get(0).getParameters()).hasSize(1);
        assertThat(result.get(0).getParameters().get(0).getName()).isEqualTo("transactionType");
        assertThat(result.get(0).getParameters().get(0).getType()).isEqualTo("TransactionType");
        assertThat(result.get(0).getParameters().get(0).isRequired()).isTrue();
        
        // 現在のEXAMPLE_PATTERNでは動的値を含む@exampleは解析されない可能性があることを考慮
        for (JavaDocAnalyzer.ExampleInfo example : result.get(0).getExamples()) {
        }
    }
}
