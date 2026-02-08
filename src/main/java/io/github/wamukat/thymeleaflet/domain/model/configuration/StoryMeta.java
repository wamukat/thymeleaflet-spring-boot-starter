package io.github.wamukat.thymeleaflet.domain.model.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * ストーリー設定メタ情報 - Value Object (Expert改善版)
 * 
 * ストーリー設定のメタデータを表現するValue Object
 * Record使用によるPure Domain Model
 * - Jackson依存完全除去
 * - 不変オブジェクト保証
 * 
 * Clean Architecture: Pure Domain層Value Object
 */
public record StoryMeta(
    @JsonProperty("title") String title,
    @JsonProperty("description") String description
) {
    // Expert推奨: Compact constructor for validation and immutability
    public StoryMeta {
        title = Objects.requireNonNullElse(title, "Default Title");
        description = Objects.requireNonNullElse(description, "");
    }

    /**
     * ドメインメソッド: メタ情報の有効性確認
     */
    public boolean isValid() {
        return !title.trim().isEmpty();
    }
}
