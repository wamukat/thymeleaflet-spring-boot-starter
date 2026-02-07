package io.github.wamukat.thymeleaflet.application.port.inbound.story;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import org.jspecify.annotations.Nullable;

/**
 * ストーリー検証専用ユースケース - Inbound Port
 * 
 * 責務: ストーリー検証のみ
 * SRP準拠: 単一責任原則に従い、ストーリー検証のみを担当
 */
public interface StoryValidationUseCase {

    /**
     * ストーリー検証
     */
    StoryValidationResult validateStory(StoryValidationCommand command);

    /**
     * ストーリー検証コマンド
     */
    class StoryValidationCommand {
        private final String templatePath;
        private final String fragmentName;
        private final String storyName;

        public StoryValidationCommand(String templatePath, String fragmentName, String storyName) {
            this.templatePath = templatePath;
            this.fragmentName = fragmentName;
            this.storyName = storyName;
        }

        public String getTemplatePath() { return templatePath; }
        public String getFragmentName() { return fragmentName; }
        public String getStoryName() { return storyName; }
    }

    /**
     * ストーリー検証結果
     */
    class StoryValidationResult {
        private final @Nullable FragmentStoryInfo story;
        private final boolean success;

        private StoryValidationResult(@Nullable FragmentStoryInfo story, boolean success) {
            this.story = story;
            this.success = success;
        }

        public static StoryValidationResult success(FragmentStoryInfo story) {
            return new StoryValidationResult(story, true);
        }

        public static StoryValidationResult failure() {
            return new StoryValidationResult(null, false);
        }

        public @Nullable FragmentStoryInfo getStory() { return story; }
        public boolean isSuccess() { return success; }
    }
}
