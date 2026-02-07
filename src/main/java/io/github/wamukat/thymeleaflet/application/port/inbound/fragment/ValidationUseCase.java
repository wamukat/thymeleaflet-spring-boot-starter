package io.github.wamukat.thymeleaflet.application.port.inbound.fragment;

import org.jspecify.annotations.Nullable;

/**
 * 汎用検証専用ユースケース - Inbound Port
 * 
 * 責務: 汎用検証のみ
 * SRP準拠: 単一責任原則に従い、汎用検証のみを担当
 */
public interface ValidationUseCase {

    /**
     * ストーリーリクエストの検証
     */
    void validateStoryRequest(ValidationCommand command);

    /**
     * フラグメント検証データのセットアップ
     */
    void setupFragmentValidationData(ValidationCommand command);

    /**
     * 検証コマンド
     */
    class ValidationCommand {
        private final @Nullable String target;
        private final @Nullable Object data;
        private final @Nullable String templatePath;
        private final @Nullable String fragmentName;
        private final @Nullable String storyName;

        public ValidationCommand(String target, Object data) {
            this.target = target;
            this.data = data;
            this.templatePath = null;
            this.fragmentName = null;
            this.storyName = null;
        }

        public ValidationCommand(String templatePath, String fragmentName, String storyName) {
            this.templatePath = templatePath;
            this.fragmentName = fragmentName;
            this.storyName = storyName;
            this.target = templatePath + "::" + fragmentName + "::" + storyName;
            this.data = null;
        }

        public @Nullable String getTarget() { return target; }
        public @Nullable Object getData() { return data; }
        public @Nullable String getTemplatePath() { return templatePath; }
        public @Nullable String getFragmentName() { return fragmentName; }
        public @Nullable String getStoryName() { return storyName; }
    }

}
