package io.github.wamukat.thymeleaflet.domain.model;

import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryItem;
import io.github.wamukat.thymeleaflet.domain.model.configuration.StoryPreview;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * フラグメント情報とストーリー情報を統合したモデル
 */
public class FragmentStoryInfo {
    
    private final FragmentSummary fragmentSummary;
    private final StoryItem story;
    private final boolean hasStoryConfig;
    private final String storyName;
    private final String fragmentGroupName;
    private final Map<String, Object> fallbackParameters;
    
    /**
     * プライベートコンストラクタ - 不変Value Object設計
     * 
     * ファクトリメソッドのみからのインスタンス化を強制
     * Clean Architecture: 検証済み値による安全なオブジェクト生成
     */
    private FragmentStoryInfo(FragmentSummary fragmentSummary,
                           String fragmentGroupName,
                           String storyName,
                           StoryItem story,
                           Map<String, Object> fallbackParameters,
                           boolean hasStoryConfig) {
        this.fragmentSummary = Objects.requireNonNull(fragmentSummary, "Fragment summary cannot be null");
        this.fragmentGroupName = Objects.requireNonNullElse(fragmentGroupName, "");
        this.storyName = Objects.requireNonNullElse(storyName, "default");
        this.hasStoryConfig = hasStoryConfig;
        this.story = Objects.requireNonNull(story, "story cannot be null");
        
        // 防御的コピー + 不変化
        this.fallbackParameters = Collections.unmodifiableMap(
            new HashMap<>(Objects.requireNonNullElse(fallbackParameters, Collections.emptyMap())));
    }
    
    /**
     * プライベートコンストラクタ (基本版)
     */
    private FragmentStoryInfo(FragmentSummary fragmentSummary,
                           String fragmentGroupName,
                           String storyName,
                           StoryItem story) {
        this(fragmentSummary, fragmentGroupName, storyName, story, 
             Collections.emptyMap(), true); // fallbackParameters
    }
    
    /**
     * FragmentStoryInfo作成 - ファクトリメソッド（完全指定版）
     */
    public static FragmentStoryInfo of(FragmentSummary fragmentSummary, 
                                     String fragmentGroupName,
                                     String storyName, 
                                     StoryItem story,
                                     Map<String, Object> fallbackParameters) {
        return new FragmentStoryInfo(
            fragmentSummary,
            fragmentGroupName,
            storyName,
            Objects.requireNonNull(story, "story cannot be null"),
            fallbackParameters,
            true
        );
    }

    /**
     * FragmentStoryInfo作成 - ファクトリメソッド（基本版）
     */
    public static FragmentStoryInfo of(FragmentSummary fragmentSummary, 
                                     String fragmentGroupName,
                                     String storyName, 
                                     StoryItem story) {
        return new FragmentStoryInfo(
            fragmentSummary,
            fragmentGroupName,
            storyName,
            Objects.requireNonNull(story, "story cannot be null")
        );
    }

    /**
     * Story設定ファイルが存在しない場合のfallbackストーリーを作成
     */
    public static FragmentStoryInfo fallback(FragmentSummary fragmentSummary,
                                             String fragmentGroupName,
                                             String storyName) {
        String resolvedStoryName = Objects.requireNonNullElse(storyName, "default");
        StoryItem implicitStory = new StoryItem(
            resolvedStoryName,
            resolvedStoryName,
            "",
            Collections.emptyMap(),
            StoryPreview.empty(),
            Collections.emptyMap()
        );
        return new FragmentStoryInfo(
            fragmentSummary,
            fragmentGroupName,
            resolvedStoryName,
            implicitStory,
            Collections.emptyMap(),
            false
        );
    }
    
    public FragmentSummary getFragmentSummary() { return fragmentSummary; }
    public StoryItem getStory() { return story; }
    public String getStoryName() { return storyName; }
    public String getFragmentGroupName() { return fragmentGroupName; }
    
    /**
     * ストーリーのパラメータを取得（フォールバック機能付き）
     */
    public Map<String, Object> getParameters() {
        if (!story.parameters().isEmpty()) {
            return story.parameters();
        }
        // フォールバックパラメータが設定されている場合はそれを返す
        if (!fallbackParameters.isEmpty()) {
            return fallbackParameters;
        }
        return Collections.emptyMap();
    }
    
    /**
     * ストーリーのパラメータを取得（短縮版）
     */
    public Map<String, Object> parameters() {
        return getParameters();
    }

    /**
     * ストーリーのモデル値を取得
     */
    public Map<String, Object> getModel() {
        return story.model();
    }

    /**
     * ストーリーのモデル値を取得（短縮版）
     */
    public Map<String, Object> model() {
        return getModel();
    }
    
    /**
     * フォールバックパラメータを設定した新しいインスタンスを返す
     */
    public FragmentStoryInfo withFallbackParameters(Map<String, Object> parameters) {
        return FragmentStoryInfo.of(fragmentSummary, fragmentGroupName, storyName, story, parameters);
    }

    /**
     * ストーリーのタイトルを取得
     */
    public String getDisplayTitle() {
        return story.title();
    }
    
    /**
     * ストーリーの説明を取得
     */
    public String getDisplayDescription() {
        return story.description();
    }
    
    /**
     * ストーリーファイルが存在するかどうか（フォールバック状態ではない）
     */
    public boolean hasStoryConfig() {
        return hasStoryConfig;
    }

}
