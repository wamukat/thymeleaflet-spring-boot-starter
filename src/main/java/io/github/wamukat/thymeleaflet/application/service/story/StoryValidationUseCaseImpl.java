package io.github.wamukat.thymeleaflet.application.service.story;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryValidationUseCase;
import io.github.wamukat.thymeleaflet.application.port.outbound.StoryDataPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ストーリー検証専用ユースケース実装
 * 
 * 責務: ストーリー検証のみ
 * SRP準拠: 単一責任原則に従い、ストーリー検証のみを担当
 */
@Component
@Transactional(readOnly = true)
public class StoryValidationUseCaseImpl implements StoryValidationUseCase {
    
    @Autowired
    private StoryDataPort storyDataPort;

    @Override
    public StoryValidationResult validateStory(StoryValidationCommand command) {
        return storyDataPort.getStory(
            command.getTemplatePath(), 
            command.getFragmentName(), 
            command.getStoryName()
        )
            .map(StoryValidationResult::success)
            .orElseGet(StoryValidationResult::failure);
    }
}
