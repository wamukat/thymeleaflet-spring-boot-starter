package io.github.wamukat.thymeleaflet.application.port.inbound.story;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;

import java.util.List;
import java.util.Optional;

/**
 * ストーリー取得専用ユースケース - Inbound Port
 * 
 * 責務: ストーリー取得のみ
 * SRP準拠: 単一責任原則に従い、ストーリー取得のみを担当
 */
public interface StoryRetrievalUseCase {

    /**
     * ストーリー取得
     */
    Optional<FragmentStoryInfo> getStory(String templatePath, String fragmentName, String storyName);

    /**
     * フラグメントのストーリー一覧取得
     */
    List<FragmentStoryInfo> getStoriesForFragment(FragmentSummary fragmentSummary);

    /**
     * フラグメント用ストーリー一覧取得
     */
    StoryListResponse getStoriesForFragment(String templatePath, String fragmentName);

    /**
     * ストーリー一覧レスポンス
     */
    class StoryListResponse {
        private final boolean success;
        private final Optional<FragmentSummary> fragment;
        private final List<FragmentStoryInfo> stories;

        public StoryListResponse(
            boolean success,
            Optional<FragmentSummary> fragment,
            List<FragmentStoryInfo> stories
        ) {
            this.success = success;
            this.fragment = fragment;
            this.stories = stories;
        }

        public static StoryListResponse success(FragmentSummary fragment, List<FragmentStoryInfo> stories) {
            return new StoryListResponse(true, Optional.of(fragment), stories);
        }

        public static StoryListResponse failure() {
            return new StoryListResponse(false, Optional.empty(), List.of());
        }

        public boolean isSuccess() { return success; }

        public Optional<FragmentSummary> getFragment() { return fragment; }
        public List<FragmentStoryInfo> getStories() { return stories; }
    }
}
