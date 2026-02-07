package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.coordination.StoryPageCoordinationUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.SecureTemplatePath;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.infrastructure.web.service.FragmentJsonService;
import io.github.wamukat.thymeleaflet.infrastructure.web.service.SecurePathConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * ストーリープレビューページ処理専用サービス
 * 
 * 責務: ストーリープレビューページ表示の詳細処理
 * SRP適用完了: HTMXコンテンツ処理はStoryContentServiceに分離済み
 */
@Component
public class StoryPreviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(StoryPreviewService.class);
    
    @Autowired
    private StoryPageCoordinationUseCase storyPageCoordinationUseCase;
    
    
    @Autowired
    private StoryRetrievalUseCase storyRetrievalUseCase;
    
    @Autowired
    private FragmentJsonService fragmentJsonService;
    
    @Autowired
    private StoryCommonDataService storyCommonDataService;
    
    @Autowired
    private SecurePathConversionService securePathConversionService;
    
    
    /**
     * ストーリープレビューページ処理
     * 
     * メインページ表示のための包括的な処理を実行
     * 
     * @param templatePath テンプレートパス (エンコード済み)
     * @param fragmentName フラグメント名
     * @param storyName ストーリー名
     * @param model Spring MVCモデル
     * @return 処理結果
     */
    public StoryPreviewResult processStoryPreview(String templatePath, String fragmentName, 
                                                 String storyName, Model model) {
        logger.info("=== StoryPreviewService.processStoryPreview START ===");
        logger.info("RAW Parameters: templatePath={}, fragmentName={}, storyName={}", templatePath, fragmentName, storyName);
        
        // セキュアパス変換
        SecurePathConversionService.SecurityConversionResult conversionResult = securePathConversionService.convertSecurePath(templatePath, model);
        if (!conversionResult.succeeded()) {
            return StoryPreviewResult.failure(
                Objects.requireNonNullElse(conversionResult.templateReference(),
                    "thymeleaflet/fragments/error-display :: error(type='danger')")
            );
        }
        String fullTemplatePath = Objects.requireNonNull(conversionResult.fullTemplatePath());
        
        // 外部仕様として必要な属性を最優先で設定（契約テスト保護）
        model.addAttribute("templatePathEncoded", templatePath.replace("/", ".")); // 契約テスト必須属性：パス変換
        model.addAttribute("fragmentName", fragmentName); // 契約テスト必須属性
        model.addAttribute("storyName", storyName); // 契約テスト必須属性
        
        // 対象ストーリーを取得
        logger.info("Calling storyManagementUseCase.getStory with: templatePath={}, fragmentName={}, storyName={}", fullTemplatePath, fragmentName, storyName);
        Optional<FragmentStoryInfo> storyInfoOptional = storyRetrievalUseCase
            .getStory(fullTemplatePath, fragmentName, storyName);

        if (storyInfoOptional.isEmpty()) {
            model.addAttribute("error", "指定されたストーリーが見つかりません: " + fullTemplatePath + "::" + fragmentName + "::" + storyName);
            // 契約テスト保護：エラー時でも必要な属性を設定
            model.addAttribute("uniquePaths", Arrays.asList("基本データ")); // Thymeleafテンプレート必須属性
            model.addAttribute("fragments", Arrays.asList()); // 空のリスト
            model.addAttribute("hierarchicalFragments", new HashMap<>());
            return StoryPreviewResult.failure("thymeleaflet/fragment-list");
        }
        FragmentStoryInfo storyInfo = storyInfoOptional.orElseThrow();
        
        // フラグメント一覧ページと同じ基本データを設定 (協調UseCase使用)
        StoryPageCoordinationUseCase.StoryPageRequest coordinationRequest = 
            new StoryPageCoordinationUseCase.StoryPageRequest(fullTemplatePath, fragmentName, storyName, model);
        StoryPageCoordinationUseCase.StoryPageResult coordinationResult = 
            storyPageCoordinationUseCase.coordinateStoryPageSetup(coordinationRequest);
        
        if (!coordinationResult.succeeded()) {
            logger.error("Story page coordination failed: {}", coordinationResult.errorMessage());
            model.addAttribute("error", coordinationResult.errorMessage());
            return StoryPreviewResult.failure("thymeleaflet/fragment-list");
        }
        
        // デバッグ: coordinateStoryPageSetup後のallFragments型を確認
        Object allFragmentsAfterCoordination = model.getAttribute("allFragments");
        logger.info("After coordination - allFragments type: {}", 
                   allFragmentsAfterCoordination != null ? allFragmentsAfterCoordination.getClass().getName() : "null");
        
        // StoryCommonDataServiceを使用して共通データ設定
        storyCommonDataService.setupCommonStoryData(fullTemplatePath, fragmentName, storyName, storyInfo, model);
        
        // 左ナビゲーション表示のためのJSON属性設定 (重要: この処理が失われていた)
        Object allFragmentsObj = model.getAttribute("allFragments");
        @SuppressWarnings("unchecked")
        Map<String, Object> hierarchicalFragments = 
            (Map<String, Object>) model.getAttribute("hierarchicalFragments");
        
        if (allFragmentsObj != null && hierarchicalFragments != null) {
            // 型安全版のsetupFragmentJsonAttributesを呼び出し
            fragmentJsonService.setupFragmentJsonAttributes(allFragmentsObj, hierarchicalFragments, model);
        }
        
        return StoryPreviewResult.success("thymeleaflet/fragment-list");
    }
    
    
    
    /**
     * ストーリープレビュー処理結果
     */
    public static class StoryPreviewResult {
        private final boolean succeeded;
        private final String templateReference;
        
        private StoryPreviewResult(boolean succeeded, String templateReference) {
            this.succeeded = succeeded;
            this.templateReference = templateReference;
        }
        
        public static StoryPreviewResult success(String templateReference) {
            return new StoryPreviewResult(true, templateReference);
        }
        
        public static StoryPreviewResult failure(String templateReference) {
            return new StoryPreviewResult(false, templateReference);
        }
        
        public boolean succeeded() { return succeeded; }
        public String templateReference() { return templateReference; }
    }
    
}
