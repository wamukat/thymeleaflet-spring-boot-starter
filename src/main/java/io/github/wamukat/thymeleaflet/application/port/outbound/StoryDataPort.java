package io.github.wamukat.thymeleaflet.application.port.outbound;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.Map;

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
    @Nullable StoryConfiguration loadStoryConfiguration(String templatePath);
    
    /**
     * ストーリー固有のパラメータを読み込む
     */
    Map<String, Object> loadStoryParameters(FragmentStoryInfo storyInfo);

    /**
     * 指定されたストーリー情報を取得
     */
    @Nullable FragmentStoryInfo getStory(String templatePath, String fragmentName, String storyName);
}
