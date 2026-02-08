package io.github.wamukat.thymeleaflet.domain.model.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * ストーリーのプレビュー設定 - Value Object
 *
 * プレビュー表示時のラッパーなど、プレビュー専用の追加設定を保持。
 */
public record StoryPreview(
    @JsonProperty("wrapper") String wrapper
) {
    public StoryPreview {
        wrapper = Objects.requireNonNullElse(wrapper, "");
    }

    public boolean hasWrapper() {
        return !wrapper.trim().isEmpty();
    }

    public static StoryPreview empty() {
        return new StoryPreview("");
    }
}
