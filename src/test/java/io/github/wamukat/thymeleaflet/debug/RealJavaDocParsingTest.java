package io.github.wamukat.thymeleaflet.debug;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.TypeInformationExtractor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    void transactionBadgeFixture_shouldParseJavaDocAndInferTransactionType() throws Exception {
        String htmlContent = readTransactionBadgeFixture();

        List<JavaDocAnalyzer.JavaDocInfo> javadocInfos = javaDocAnalyzer.analyzeJavaDocFromHtml(htmlContent);
        List<TypeInfo> typeInfos = typeInformationExtractor.extractTypeInformationFromHtml(htmlContent);

        assertThat(javadocInfos).hasSize(1);
        JavaDocAnalyzer.JavaDocInfo info = javadocInfos.get(0);
        assertThat(info.getDescription()).contains("ポイント取引タイプバッジフラグメント");
        assertThat(info.getParameters()).hasSize(1);
        assertThat(info.getParameters().get(0).getName()).isEqualTo("transactionType");
        assertThat(info.getParameters().get(0).getType()).isEqualTo("TransactionType");
        assertThat(info.getExamples()).hasSize(1);

        assertThat(typeInfos)
            .filteredOn(typeInfo -> "transactionType".equals(typeInfo.getParameterName()))
            .singleElement()
            .satisfies(typeInfo -> {
                assertThat(typeInfo.getJavaTypeName()).isEqualTo("TransactionType");
                assertThat(typeInfo.getTypeCategory()).isEqualTo(TypeInfo.TypeCategory.ENUM);
                assertThat(typeInfo.getAllowedValues()).contains("EARN", "USE");
            });
    }
    
    @Test
    void transactionBadgeFixture_shouldKeepExpectedSourceMarkers() throws Exception {
        String htmlContent = readTransactionBadgeFixture();

        assertThat(htmlContent).contains("/**", "*/", "@param", "TransactionType");
        assertThat(htmlContent).contains("th:fragment=\"transactionTypeBadge(transactionType)\"");
    }

    private static String readTransactionBadgeFixture() throws Exception {
        try (InputStream inputStream = RealJavaDocParsingTest.class.getResourceAsStream(
            "/templates/domain/point/point-transaction-badge.html"
        )) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
