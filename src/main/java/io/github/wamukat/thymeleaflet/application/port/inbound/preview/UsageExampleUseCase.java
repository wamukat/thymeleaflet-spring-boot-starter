package io.github.wamukat.thymeleaflet.application.port.inbound.preview;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;

import java.util.Map;

/**
 * 使用例生成専用ユースケース - Inbound Port
 * 
 * 責務: 使用例生成のみ
 * SRP準拠: 単一責任原則に従い、使用例生成のみを担当
 */
public interface UsageExampleUseCase {

    /**
     * フラグメント使用例を生成
     */
    UsageExampleResult generateUsageExample(UsageExampleCommand command);

    /**
     * 使用例生成処理（StoryManagementUseCaseから移行）
     */
    UsageExampleResponse generateUsageExample(FragmentStoryInfo storyInfo, Map<String, Object> parameters);

    /**
     * エラー使用例を生成
     */
    ErrorUsageExampleResponse generateErrorUsageExample(FragmentStoryInfo storyInfo);

    // === Command Objects ===

    /**
     * 使用例生成コマンド
     */
    class UsageExampleCommand {
        private final String templatePath;
        private final String fragmentName;
        private final String storyName;

        public UsageExampleCommand(String templatePath, String fragmentName, String storyName) {
            this.templatePath = templatePath;
            this.fragmentName = fragmentName;
            this.storyName = storyName;
        }

        public String getTemplatePath() { return templatePath; }
        public String getFragmentName() { return fragmentName; }
        public String getStoryName() { return storyName; }
    }

    // === Result Objects ===

    /**
     * 使用例生成結果
     */
    class UsageExampleResult {
        private final boolean success;

        private UsageExampleResult(boolean success) {
            this.success = success;
        }

        public static UsageExampleResult success() {
            return new UsageExampleResult(true);
        }

        public static UsageExampleResult failure() {
            return new UsageExampleResult(false);
        }

        public boolean isSuccess() { return success; }
    }

    /**
     * 使用例レスポンス（StoryManagementUseCaseから移行）
     */
    class UsageExampleResponse {
        private final String usageExample;
        private final String fragmentName;
        private final String storyName;

        public UsageExampleResponse(String usageExample, String fragmentName, String storyName) {
            this.usageExample = usageExample;
            this.fragmentName = fragmentName;
            this.storyName = storyName;
        }

        public static UsageExampleResponse success(String usageExample, String fragmentName, String storyName) {
            return new UsageExampleResponse(usageExample, fragmentName, storyName);
        }

        public String getUsageExample() { return usageExample; }
        public String getFragmentName() { return fragmentName; }
        public String getStoryName() { return storyName; }
    }

    /**
     * エラー使用例レスポンス
     */
    class ErrorUsageExampleResponse {
        private final String errorUsageExample;

        public ErrorUsageExampleResponse(String errorUsageExample) {
            this.errorUsageExample = errorUsageExample;
        }

        public String getErrorUsageExample() { return errorUsageExample; }
    }
}