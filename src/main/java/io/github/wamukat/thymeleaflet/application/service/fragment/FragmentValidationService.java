package io.github.wamukat.thymeleaflet.application.service.fragment;

import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.ValidationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * フラグメント検証サービス
 * 
 * 責務: フラグメント検証のみ
 * SRP準拠: 単一責任原則に従い、フラグメント検証のみを担当
 */
@Component
@Transactional(readOnly = true)
public class FragmentValidationService implements ValidationUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(FragmentValidationService.class);

    @Override
    public void validateStoryRequest(ValidationCommand command) {
        logger.info("Validating story request: {}", command.getTarget().orElse("unknown"));
        
        // ストーリーリクエストの検証ロジック
        if (command.getTemplatePath().isEmpty() || command.getTemplatePath().orElseThrow().trim().isEmpty()) {
            throw new IllegalArgumentException("Template path cannot be null or empty");
        }
        if (command.getFragmentName().isEmpty() || command.getFragmentName().orElseThrow().trim().isEmpty()) {
            throw new IllegalArgumentException("Fragment name cannot be null or empty");
        }
        if (command.getStoryName().isEmpty() || command.getStoryName().orElseThrow().trim().isEmpty()) {
            throw new IllegalArgumentException("Story name cannot be null or empty");
        }
        
        logger.info("Story request validation passed for: {}", command.getTarget().orElse("unknown"));
    }

    @Override
    public void setupFragmentValidationData(ValidationCommand command) {
        logger.info("Setting up fragment validation data for: {}", command.getTarget().orElse("unknown"));
        
        // フラグメント検証データのセットアップ（プレースホルダー実装）
        // 現在は特に処理なし - 必要に応じて将来拡張
        
        logger.info("Fragment validation data setup completed for: {}", command.getTarget().orElse("unknown"));
    }
}
