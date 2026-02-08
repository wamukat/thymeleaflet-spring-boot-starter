package io.github.wamukat.thymeleaflet.application.port.inbound.fragment;

import java.util.Optional;

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
        private final Optional<String> target;
        private final Optional<Object> data;
        private final Optional<String> templatePath;
        private final Optional<String> fragmentName;
        private final Optional<String> storyName;

        public ValidationCommand(String target, Object data) {
            this.target = Optional.of(target);
            this.data = Optional.of(data);
            this.templatePath = Optional.empty();
            this.fragmentName = Optional.empty();
            this.storyName = Optional.empty();
        }

        public ValidationCommand(String templatePath, String fragmentName, String storyName) {
            this.templatePath = Optional.of(templatePath);
            this.fragmentName = Optional.of(fragmentName);
            this.storyName = Optional.of(storyName);
            this.target = Optional.of(templatePath + "::" + fragmentName + "::" + storyName);
            this.data = Optional.empty();
        }

        public Optional<String> getTarget() { return target; }
        public Optional<Object> getData() { return data; }
        public Optional<String> getTemplatePath() { return templatePath; }
        public Optional<String> getFragmentName() { return fragmentName; }
        public Optional<String> getStoryName() { return storyName; }
    }

}
