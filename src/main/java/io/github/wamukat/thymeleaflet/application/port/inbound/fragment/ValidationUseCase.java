package io.github.wamukat.thymeleaflet.application.port.inbound.fragment;

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
        private final String target;
        private final Object data;
        private final String templatePath;
        private final String fragmentName;
        private final String storyName;

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

        public String getTarget() { return target; }
        public Object getData() { return data; }
        public String getTemplatePath() { return templatePath; }
        public String getFragmentName() { return fragmentName; }
        public String getStoryName() { return storyName; }
    }

}