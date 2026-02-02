package io.github.wamukat.thymeleaflet.application.port.inbound.coordination;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

/**
 * ストーリーコンテンツ協調ユースケース - Inbound Port
 * 
 * HTMXコンテンツフラグメント表示に必要な複数UseCaseの協調処理を定義
 * StoryPreviewController.storyContentFragment()の複雑性を軽減
 */
public interface StoryContentCoordinationUseCase {
    
    /**
     * ストーリーコンテンツセットアップ用リクエスト
     */
    record StoryContentRequest(
        String fullTemplatePath,
        String fragmentName,
        String storyName,
        Model model
    ) {}
    
    /**
     * ストーリーコンテンツセットアップ用レスポンス
     */
    record StoryContentResult(
        boolean succeeded,
        String errorMessage,
        FragmentStoryInfo storyInfo,
        FragmentDiscoveryService.FragmentInfo selectedFragment,
        List<FragmentStoryInfo> stories,
        Map<String, Object> displayParameters,
        FragmentStoryInfo defaultStory,
        Map<String, Object> defaultParameters,
        Object javadocInfo
    ) {
        public static StoryContentResult success(
            FragmentStoryInfo storyInfo,
            FragmentDiscoveryService.FragmentInfo selectedFragment,
            List<FragmentStoryInfo> stories,
            Map<String, Object> displayParameters,
            FragmentStoryInfo defaultStory,
            Map<String, Object> defaultParameters,
            Object javadocInfo
        ) {
            return new StoryContentResult(true, null, storyInfo, selectedFragment, stories,
                displayParameters, defaultStory, defaultParameters, javadocInfo);
        }
        
        public static StoryContentResult failure(String errorMessage) {
            return new StoryContentResult(false, errorMessage, null, null, null, null, null, null, null);
        }
    }
    
    /**
     * ストーリーコンテンツ表示のための協調処理
     * 
     * 以下の処理を統合実行:
     * 1. ストーリー検証
     * 2. フラグメント情報取得
     * 3. ストーリー一覧取得
     * 4. 共通データセットアップ (パラメータ・JavaDoc・デフォルト比較)
     * 
     * @param request ストーリーコンテンツリクエスト
     * @return 協調処理結果
     */
    StoryContentResult coordinateStoryContentSetup(StoryContentRequest request);
}