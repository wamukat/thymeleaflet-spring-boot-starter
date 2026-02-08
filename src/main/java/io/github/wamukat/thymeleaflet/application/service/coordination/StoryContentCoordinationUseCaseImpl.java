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
import java.util.Objects;
import java.util.Optional;
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
            if (!validationResult.isSuccess()) {
                return StoryContentResult.failure("Story not found");
            }
            FragmentStoryInfo storyInfo = validationResult.getStory().orElseThrow();
            
            // 2. フラグメント情報取得
            List<FragmentDiscoveryService.FragmentInfo> allFragments = fragmentDiscoveryService.discoverFragments();
            var selectedFragment = thymeleafFragmentRenderer
                .findFragmentByIdentifier(allFragments, request.fullTemplatePath(), request.fragmentName());
            
            if (selectedFragment.isEmpty()) {
                return StoryContentResult.failure("Fragment not found: " + request.fullTemplatePath() + "::" + request.fragmentName());
            }
            FragmentDiscoveryService.FragmentInfo fragmentInfo = selectedFragment.orElseThrow();
            
            // 3. ストーリー一覧取得
            List<FragmentStoryInfo> stories = storyRetrievalUseCase.getStoriesForFragment(fragmentInfo);
            
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
            Optional<FragmentStoryInfo> defaultStory = Optional.of(storyInfo);
            Map<String, Object> defaultParameters = new HashMap<>();
            
            if (!request.storyName().equals("default")) {
                var defaultStoryOptional = storyRetrievalUseCase
                    .getStory(request.fullTemplatePath(), request.fragmentName(), "default");
                if (defaultStoryOptional.isPresent()) {
                    FragmentStoryInfo defaultStoryInfo = defaultStoryOptional.orElseThrow();
                    defaultStory = Optional.of(defaultStoryInfo);
                    Map<String, Object> defaultStoryParams = storyParameterUseCase.getParametersForStory(defaultStoryInfo);
                    defaultParameters = defaultStoryParams.entrySet().stream()
                        .filter(entry -> !"__storybook_background".equals(entry.getKey()))
                        .filter(entry -> Objects.nonNull(entry.getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            }
            
            // JavaDoc情報取得
            var javadocInfo = javaDocLookupService.findJavaDocInfo(request.fullTemplatePath(), request.fragmentName());
            
            logger.info("=== StoryContentCoordination COMPLETED ===");
            return StoryContentResult.success(storyInfo, fragmentInfo, stories,
                displayParameters, defaultStory.orElseThrow(), defaultParameters, javadocInfo);
            
        } catch (Exception e) {
            logger.error("Story content coordination failed", e);
            return StoryContentResult.failure("コンテンツセットアップに失敗しました: " + e.getMessage());
        }
    }
}
