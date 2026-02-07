package io.github.wamukat.thymeleaflet.domain.model.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * ストーリーグループ - Value Object (Expert改善版)
 * 
 * 関連するストーリーをグループ化するValue Object
 * Record使用によるPure Domain Model
 * - Jackson依存完全除去
 * - 不変オブジェクト保証
 * 
 * 例: primary, secondary, experimental
 * Clean Architecture: Pure Domain層Value Object
 */
public record StoryGroup(
    @JsonProperty("title") String title,
    @JsonProperty("description") String description,
    @JsonProperty("stories") List<StoryItem> stories
) {
    // Expert推奨: Compact constructor for validation and immutability
    public StoryGroup {
        title = title != null ? title : "Default Group";
        description = description != null ? description : "";
        stories = stories != null ? 
            Collections.unmodifiableList(stories) : Collections.emptyList();
    }

    /**
     * ドメインメソッド: ストーリー数取得
     */
    public int getStoryCount() {
        return stories.size();
    }
    
    /**
     * ドメインメソッド: 名前でストーリー検索
     */
    public Optional<StoryItem> findStoryByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        
        return stories.stream()
            .filter(story -> story.name().equals(name))
            .findFirst();
    }
    
    /**
     * ドメインメソッド: グループの有効性確認
     */
    public boolean isValid() {
        return !title.trim().isEmpty() && !stories.isEmpty();
    }
}
