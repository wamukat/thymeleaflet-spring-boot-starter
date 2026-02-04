package io.github.wamukat.thymeleaflet.application.service.coordination;

import io.github.wamukat.thymeleaflet.application.port.inbound.coordination.StoryContentCoordinationUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryValidationUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.web.rendering.ThymeleafFragmentRenderer;
import io.github.wamukat.thymeleaflet.infrastructure.web.service.JavaDocLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ストーリーコンテンツ協調ユースケース実装
 * 
 * HTMXコンテンツフラグメント表示の複雑な協調処理をApplication層で実施
 * StoryPreviewController.storyContentFragment()肥大化問題の解決を目的とする
 */
@Component
@Transactional(readOnly = true)
public class StoryContentCoordinationUseCaseImpl implements StoryContentCoordinationUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(StoryContentCoordinationUseCaseImpl.class);
    
    @Autowired
    private StoryValidationUseCase storyValidationUseCase;
    
    @Autowired
    private FragmentDiscoveryService fragmentDiscoveryService;
    
    @Autowired
    private ThymeleafFragmentRenderer thymeleafFragmentRenderer;
    
    @Autowired
    private StoryRetrievalUseCase storyRetrievalUseCase;
    
    @Autowired
    private StoryParameterUseCase storyParameterUseCase;
    
    @Autowired
    private JavaDocLookupService javaDocLookupService;
    
    @Override
    public StoryContentResult coordinateStoryContentSetup(StoryContentRequest request) {
        logger.info("=== StoryContentCoordination START ===");
        logger.info("Coordinating content setup for: {}::{}::{}", 
                   request.fullTemplatePath(), request.fragmentName(), request.storyName());
        
        try {
            // 1. ストーリー検証
            StoryValidationUseCase.StoryValidationCommand validationCommand = 
                new StoryValidationUseCase.StoryValidationCommand(request.fullTemplatePath(), request.fragmentName(), request.storyName());
            StoryValidationUseCase.StoryValidationResult validationResult = 
                storyValidationUseCase.validateStory(validationCommand);
            FragmentStoryInfo storyInfo = validationResult.getStory();
            
            if (storyInfo == null) {
                return StoryContentResult.failure("Story not found");
            }
            
            // 2. フラグメント情報取得
            List<FragmentDiscoveryService.FragmentInfo> allFragments = fragmentDiscoveryService.discoverFragments();
            FragmentDiscoveryService.FragmentInfo selectedFragment = thymeleafFragmentRenderer
                .findFragmentByIdentifier(allFragments, request.fullTemplatePath(), request.fragmentName());
            
            if (selectedFragment == null) {
                return StoryContentResult.failure("Fragment not found: " + request.fullTemplatePath() + "::" + request.fragmentName());
            }
            
            // 3. ストーリー一覧取得
            List<FragmentStoryInfo> stories = storyRetrievalUseCase.getStoriesForFragment(selectedFragment);
            
            // 4. 共通データセットアップ
            Map<String, Object> storyParameters = storyParameterUseCase.getParametersForStory(storyInfo);
            
            // フォールバックパラメータをstoryInfoオブジェクトに設定
            if (!storyInfo.hasStoryConfig() && !storyParameters.isEmpty()) {
                storyInfo = storyInfo.withFallbackParameters(storyParameters);
                logger.info("Set fallback parameters to storyInfo: {}", storyParameters.keySet());
            }
            
            // パラメータをモデルに設定し、表示用パラメータを取得
            Map<String, Object> displayParameters = thymeleafFragmentRenderer.configureModelWithStoryParameters(storyParameters, request.model());
            
            // defaultストーリーの情報取得（差異ハイライト用）
            FragmentStoryInfo defaultStory = null;
            Map<String, Object> defaultParameters = new HashMap<>();
            
            if (!request.storyName().equals("default")) {
                defaultStory = storyRetrievalUseCase.getStory(request.fullTemplatePath(), request.fragmentName(), "default");
                if (defaultStory != null) {
                    Map<String, Object> defaultStoryParams = storyParameterUseCase.getParametersForStory(defaultStory);
                    defaultParameters = defaultStoryParams.entrySet().stream()
                        .filter(entry -> !"__storybook_background".equals(entry.getKey()))
                        .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            }
            
            // JavaDoc情報取得
            Object javadocInfo = javaDocLookupService.findJavaDocInfo(request.fullTemplatePath(), request.fragmentName());
            
            logger.info("=== StoryContentCoordination COMPLETED ===");
            return StoryContentResult.success(storyInfo, selectedFragment, stories, 
                displayParameters, defaultStory, defaultParameters, javadocInfo);
            
        } catch (Exception e) {
            logger.error("Story content coordination failed", e);
            return StoryContentResult.failure("コンテンツセットアップに失敗しました: " + e.getMessage());
        }
    }
}
