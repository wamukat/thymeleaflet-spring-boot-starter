package io.github.wamukat.thymeleaflet.application.service.story;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.service.StoryParameterDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ストーリーパラメータ管理専用ユースケース実装
 * 
 * 責務: パラメータ管理のみ
 * SRP準拠: 単一責任原則に従い、ストーリーパラメータ管理のみを担当
 */
@Component
@Transactional(readOnly = true)
public class StoryParameterUseCaseImpl implements StoryParameterUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(StoryParameterUseCaseImpl.class);
    
    @Autowired
    private StoryParameterDomainService storyParameterDomainService;

    @Override
    public Map<String, Object> getParametersForStory(FragmentStoryInfo storyInfo) {
        return storyParameterDomainService.generateStoryParameters(storyInfo);
    }

    @Override
    public ParameterExtractionResponse extractRelevantParameters(ParameterExtractionCommand command) {
        Map<String, Object> relevantParams = new HashMap<>();
        
        // フラグメントのパラメータリストを取得
        List<String> fragmentParams = command.getFragment().getParameters();
        if (fragmentParams == null || fragmentParams.isEmpty()) {
            return ParameterExtractionResponse.success(relevantParams);
        }
        
        // フラグメントが実際に要求するパラメータのみを抽出
        int extractedCount = 0;
        for (String paramName : fragmentParams) {
            if (command.getAllModelData().containsKey(paramName)) {
                Object value = command.getAllModelData().get(paramName);
                relevantParams.put(paramName, value);
                logger.info("Extracted parameter: {} = {}", paramName, value);
                extractedCount++;
            } else {
                logger.warn("Required parameter '{}' not found in model data", paramName);
            }
        }
        
        return ParameterExtractionResponse.success(relevantParams);
    }

    @Override
    public ParameterExtractionResponse extractRelevantParameters(ParameterExtractionApplicationCommand command) {
        // ParameterExtractionApplicationCommandからParameterExtractionCommandに変換
        ParameterExtractionCommand standardCommand = new ParameterExtractionCommand(
            command.getFragment(),
            command.getAllModelData()
        );
        
        // 既存のextractRelevantParametersメソッドを再利用
        return extractRelevantParameters(standardCommand);
    }
}