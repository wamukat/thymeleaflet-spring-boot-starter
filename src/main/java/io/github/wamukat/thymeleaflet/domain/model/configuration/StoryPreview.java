package io.github.wamukat.thymeleaflet.domain.model.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * ストーリーのプレビュー設定 - Value Object
 *
 * プレビュー表示時のラッパーなど、プレビュー専用の追加設定を保持。
 */
public record StoryPreview(
    @JsonProperty("wrapper") String wrapper,
    @JsonProperty("viewport") String viewport,
    @JsonProperty("minHeight") int minHeight
) {
    public StoryPreview {
        wrapper = Objects.requireNonNullElse(wrapper, "");
        viewport = Objects.requireNonNullElse(viewport, "");
    }

    public StoryPreview(String wrapper, String viewport) {
        this(wrapper, viewport, 0);
    }

    public StoryPreview(String wrapper) {
        this(wrapper, "", 0);
    }

    public boolean hasWrapper() {
        return !wrapper.trim().isEmpty();
    }

    public boolean hasViewport() {
        return !viewport.trim().isEmpty();
    }

    public boolean hasMinHeight() {
        return minHeight > 0;
    }

    public static StoryPreview empty() {
        return new StoryPreview("", "", 0);
    }
}
