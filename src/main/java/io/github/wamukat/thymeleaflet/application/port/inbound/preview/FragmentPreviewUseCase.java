package io.github.wamukat.thymeleaflet.application.port.inbound.preview;

import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * フラグメントプレビュー専用ユースケース - Inbound Port
 * 
 * 責務: フラグメントプレビューのみ
 * SRP準拠: 単一責任原則に従い、フラグメントプレビューのみを担当
 */
public interface FragmentPreviewUseCase {

    /**
     * ページセットアップ処理（既存メソッド保持）
     */
    PageSetupResponse setupStoryContentData(PageSetupCommand command);

    /**
     * JSON変換処理（既存メソッド保持）
     */
    void setupFragmentJsonAttributes(
        List<Map<String, Object>> enrichedFragments,
        List<Map<String, Object>> hierarchicalFragmentsList,
        Model model
    );

    /**
     * JavaDoc情報取得
     */
    JavaDocInfoResponse getJavaDocInfoWithDetailedLogging(JavaDocInfoCommand command);

    /**
     * JavaDoc情報直接取得
     */
    Optional<JavaDocAnalyzer.JavaDocInfo> getJavaDocInfo(String templatePath, String fragmentName);

    // === Command Objects ===

    /**
     * ページセットアップコマンド
     */
    record PageSetupCommand(
        String templatePath,
        String fragmentName, 
        String storyName,
        Model model
    ) {
        public String getFullTemplatePath() { return templatePath; }
        public String getFragmentName() { return fragmentName; }
        public String getStoryName() { return storyName; }
        public org.springframework.ui.Model getModel() { return model; }
    }

    /**
     * JavaDoc情報コマンド
     */
    record JavaDocInfoCommand(
        String templatePath,
        String fragmentName
    ) {}

    /**
     * ページセットアップレスポンス
     */
    class PageSetupResponse {
        private final Optional<String> errorMessage;

        private PageSetupResponse(Optional<String> errorMessage) {
            this.errorMessage = errorMessage;
        }
        public static PageSetupResponse success() {
            return new PageSetupResponse(Optional.empty());
        }

        public static PageSetupResponse failure(String errorMessage) {
            return new PageSetupResponse(Optional.of(errorMessage));
        }

        public boolean isSucceeded() {
            return errorMessage.isEmpty();
        }

        public Optional<String> errorMessage() { return errorMessage; }
    }

    /**
     * JavaDoc情報レスポンス
     */
    class JavaDocInfoResponse {
        private final boolean hasJavaDoc;

        public JavaDocInfoResponse(boolean hasJavaDoc) {
            this.hasJavaDoc = hasJavaDoc;
        }

        public boolean hasJavaDoc() { return hasJavaDoc; }
    }
}
