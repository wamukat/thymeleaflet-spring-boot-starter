package io.github.wamukat.thymeleaflet.domain.model.configuration;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * ストーリー設定 - ドメインValue Object (Expert改善版)
 * 
 * Record使用によるPure Domain Model
 * - Jackson依存完全除去
 * - 不変オブジェクト保証
 * - Expert推奨パターン適用
 * 
 * Clean Architecture: Pure Domain層Value Object
 * 
 * Jackson annotations are kept to map YAML/JSON story files directly.
 */
public record StoryConfiguration(
    @JsonProperty("meta") StoryMeta meta,
    @JsonProperty("storyGroups") @JsonAlias("fragments") Map<String, StoryGroup> storyGroups
) {
    // Expert推奨: Compact constructor for validation and immutability
    public StoryConfiguration {
        meta = meta != null ? meta : new StoryMeta("Default Title", "");
        storyGroups = storyGroups != null ? 
            Collections.unmodifiableMap(storyGroups) : Collections.emptyMap();
    }

    /**
     * ドメインメソッド: ストーリーグループ取得
     */
    public Optional<StoryGroup> getStoryGroup(String groupName) {
        return Optional.ofNullable(storyGroups.get(groupName));
    }
    
    /**
     * ドメインメソッド: 総ストーリー数計算
     */
    public int getTotalStoryCount() {
        return storyGroups.values().stream()
            .mapToInt(group -> group.stories().size())
            .sum();
    }
    
    /**
     * ドメインメソッド: 設定の有効性確認
     */
    public boolean isValid() {
        return !storyGroups.isEmpty();
    }
}
