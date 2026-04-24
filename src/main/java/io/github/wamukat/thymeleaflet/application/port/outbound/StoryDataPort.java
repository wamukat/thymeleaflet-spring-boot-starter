package io.github.wamukat.thymeleaflet.application.port.outbound;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;

import java.util.Map;
import java.util.Optional;

/**
 * ストーリーデータアクセスポート
 * 
 * Clean Architectureの依存関係逆転原則に従い、
 * ドメイン層がInfrastructure層の実装に依存しないよう抽象化
 */
public interface StoryDataPort {
    
    /**
     * ストーリー設定を読み込む
     */
    Optional<StoryConfiguration> loadStoryConfiguration(String templatePath);

    /**
     * ストーリー設定読み込み時の診断情報を取得する。
     */
    default Optional<StoryConfigurationDiagnostic> getStoryConfigurationDiagnostic(String templatePath) {
        return Optional.empty();
    }
    
    /**
     * ストーリー固有のパラメータを読み込む
     */
    Map<String, Object> loadStoryParameters(FragmentStoryInfo storyInfo);

    /**
     * 指定されたストーリー情報を取得
     */
    Optional<FragmentStoryInfo> getStory(String templatePath, String fragmentName, String storyName);

    record StoryConfigurationDiagnostic(
        String code,
        String userSafeMessage,
        String developerMessage
    ) {}
}
