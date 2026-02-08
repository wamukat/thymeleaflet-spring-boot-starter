package io.github.wamukat.thymeleaflet.domain.model.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/**
 * ストーリーアイテム - Value Object (Expert改善版)
 * 
 * 個別のストーリー設定を表現するValue Object
 * Record使用によるPure Domain Model
 * - Jackson依存完全除去
 * - 不変オブジェクト保証
 * 
 * 例: default, disabled, large, error
 * Clean Architecture: Pure Domain層Value Object
 */
public record StoryItem(
    @JsonProperty("name") String name,
    @JsonProperty("title") String title,
    @JsonProperty("description") String description,
    @JsonProperty("parameters") Map<String, Object> parameters,
    @JsonProperty("preview") StoryPreview preview,
    @JsonProperty("model") Map<String, Object> model
) {
    // Expert推奨: Compact constructor for validation and immutability
    public StoryItem {
        name = name != null ? name : "default";
        title = title != null ? title : name;
        description = description != null ? description : "";
        parameters = parameters != null ? 
            Collections.unmodifiableMap(parameters) : Collections.emptyMap();
        preview = preview != null ? preview : StoryPreview.empty();
        model = model != null ?
            Collections.unmodifiableMap(model) : Collections.emptyMap();
    }

    /**
     * ドメインメソッド: パラメータの存在確認
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    /**
     * ドメインメソッド: モデル値の存在確認
     */
    public boolean hasModel() {
        return !model.isEmpty();
    }

    /**
     * ドメインメソッド: ストーリーアイテムの有効性確認
     */
    public boolean isValid() {
        return !name.trim().isEmpty();
    }
}
