package io.github.wamukat.thymeleaflet.application.port.inbound.story;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;

import java.util.Map;

/**
 * ストーリーパラメータ管理専用ユースケース - Inbound Port
 * 
 * 責務: パラメータ管理のみ
 * SRP準拠: 単一責任原則に従い、ストーリーパラメータ管理のみを担当
 */
public interface StoryParameterUseCase {

    /**
     * ストーリーパラメータ取得
     */
    Map<String, Object> getParametersForStory(FragmentStoryInfo storyInfo);

    /**
     * フラグメントの定義から関連するパラメータを抽出
     */
    ParameterExtractionResponse extractRelevantParameters(ParameterExtractionCommand command);

    /**
     * フラグメントの定義から関連するパラメータを抽出（Application用）
     */
    ParameterExtractionResponse extractRelevantParameters(ParameterExtractionApplicationCommand command);

    /**
     * パラメータ抽出コマンド
     */
    class ParameterExtractionCommand {
        private final FragmentSummary fragment;
        private final Map<String, Object> allModelData;

        public ParameterExtractionCommand(FragmentSummary fragment, Map<String, Object> allModelData) {
            this.fragment = fragment;
            this.allModelData = allModelData;
        }

        public FragmentSummary getFragment() { return fragment; }
        public Map<String, Object> getAllModelData() { return allModelData; }
    }

    /**
     * パラメータ抽出Applicationコマンド
     */
    class ParameterExtractionApplicationCommand {
        private final FragmentSummary fragment;
        private final Map<String, Object> allModelData;

        public ParameterExtractionApplicationCommand(FragmentSummary fragment, Map<String, Object> allModelData) {
            this.fragment = fragment;
            this.allModelData = allModelData;
        }

        public FragmentSummary getFragment() { return fragment; }
        public Map<String, Object> getAllModelData() { return allModelData; }
    }

    /**
     * パラメータ抽出レスポンス
     */
    class ParameterExtractionResponse {
        private final boolean success;
        private final Map<String, Object> parameters;

        public ParameterExtractionResponse(boolean success, Map<String, Object> parameters) {
            this.success = success;
            this.parameters = parameters;
        }

        public static ParameterExtractionResponse success(Map<String, Object> parameters) {
            return new ParameterExtractionResponse(true, parameters);
        }

        public boolean isSuccess() { return success; }
        public Map<String, Object> getParameters() { return parameters; }
    }
}
