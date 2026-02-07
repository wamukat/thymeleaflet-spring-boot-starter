package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.coordination.StoryContentCoordinationUseCase;
import io.github.wamukat.thymeleaflet.domain.model.SecureTemplatePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.Objects;

/**
 * ストーリーコンテンツHTMX処理専用サービス
 * 
 * 責務: HTMX部分更新のためのコンテンツ処理
 * StoryPreviewService SRP違反解決のための責務分離
 */
@Component
public class StoryContentService {
    
    private static final Logger logger = LoggerFactory.getLogger(StoryContentService.class);
    
    @Autowired
    private StoryContentCoordinationUseCase storyContentCoordinationUseCase;
    
    @Autowired
    private StoryCommonDataService storyCommonDataService;
    
    @Autowired
    private SecurePathConversionService securePathConversionService;
    
    /**
     * ストーリーコンテンツHTMX処理
     * 
     * HTMX部分更新のためのコンテンツ処理を実行
     * 
     * @param templatePath テンプレートパス (エンコード済み)
     * @param fragmentName フラグメント名
     * @param storyName ストーリー名
     * @param model Spring MVCモデル
     * @return 処理結果
     */
    public StoryContentResult processStoryContent(String templatePath, String fragmentName, 
                                                 String storyName, Model model) {
        logger.info("=== StoryContentService.processStoryContent START ===");
        logger.info("RAW Parameters: templatePath={}, fragmentName={}, storyName={}", templatePath, fragmentName, storyName);
        
        // セキュアパス変換
        SecurePathConversionService.SecurityConversionResult conversionResult = securePathConversionService.convertSecurePath(templatePath, model);
        if (!conversionResult.succeeded()) {
            return StoryContentResult.failure(
                Objects.requireNonNullElse(conversionResult.templateReference(),
                    "thymeleaflet/fragments/error-display :: error(type='danger')")
            );
        }
        String fullTemplatePath = Objects.requireNonNull(conversionResult.fullTemplatePath());
        
        // ストーリーコンテンツ協調処理 (協調UseCase使用)
        StoryContentCoordinationUseCase.StoryContentRequest contentRequest = 
            new StoryContentCoordinationUseCase.StoryContentRequest(fullTemplatePath, fragmentName, storyName, model);
        StoryContentCoordinationUseCase.StoryContentResult contentResult = 
            storyContentCoordinationUseCase.coordinateStoryContentSetup(contentRequest);
        
        if (!contentResult.succeeded()) {
            logger.error("Story content coordination failed: {}", contentResult.errorMessage());
            model.addAttribute("error", contentResult.errorMessage());
            return StoryContentResult.failure("thymeleaflet/fragments/error-display :: error(type='danger')");
        }
        
        // モデルに協調処理結果を設定
        model.addAttribute("selectedFragment", contentResult.selectedFragment());
        model.addAttribute("selectedStory", contentResult.storyInfo());
        model.addAttribute("storyInfo", contentResult.storyInfo());
        model.addAttribute("stories", contentResult.stories());
        
        // StoryCommonDataServiceを使用して共通データ設定
        storyCommonDataService.setupCommonStoryData(
            fullTemplatePath,
            fragmentName,
            storyName,
            Objects.requireNonNull(contentResult.storyInfo()),
            model
        );
        
        SecureTemplatePath secureTemplatePath = SecureTemplatePath.createUnsafe(templatePath);
        model.addAttribute("templatePathEncoded", secureTemplatePath.forUrl());
        
        return StoryContentResult.success("thymeleaflet/fragments/main-content :: content");
    }
    
    /**
     * ストーリーコンテンツ処理結果
     */
    public static class StoryContentResult {
        private final boolean succeeded;
        private final String templateReference;
        
        private StoryContentResult(boolean succeeded, String templateReference) {
            this.succeeded = succeeded;
            this.templateReference = templateReference;
        }
        
        public static StoryContentResult success(String templateReference) {
            return new StoryContentResult(true, templateReference);
        }
        
        public static StoryContentResult failure(String templateReference) {
            return new StoryContentResult(false, templateReference);
        }
        
        public boolean succeeded() { return succeeded; }
        public String templateReference() { return templateReference; }
    }
}
