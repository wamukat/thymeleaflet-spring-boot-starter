package io.github.wamukat.thymeleaflet.application.port.inbound.coordination;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        Optional<String> errorMessage,
        Optional<FragmentStoryInfo> storyInfo,
        Optional<FragmentDiscoveryService.FragmentInfo> selectedFragment,
        Optional<List<FragmentStoryInfo>> stories,
        Optional<Map<String, Object>> displayParameters,
        Optional<FragmentStoryInfo> defaultStory,
        Optional<Map<String, Object>> defaultParameters,
        Optional<Object> javadocInfo
    ) {
        public static StoryContentResult success(
            FragmentStoryInfo storyInfo,
            FragmentDiscoveryService.FragmentInfo selectedFragment,
            List<FragmentStoryInfo> stories,
            Map<String, Object> displayParameters,
            FragmentStoryInfo defaultStory,
            Map<String, Object> defaultParameters,
            Optional<?> javadocInfo
        ) {
            return new StoryContentResult(
                true,
                Optional.empty(),
                Optional.of(storyInfo),
                Optional.of(selectedFragment),
                Optional.of(stories),
                Optional.of(displayParameters),
                Optional.of(defaultStory),
                Optional.of(defaultParameters),
                javadocInfo.map(value -> (Object) value)
            );
        }
        
        public static StoryContentResult failure(String errorMessage) {
            return new StoryContentResult(
                false,
                Optional.of(errorMessage),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
            );
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
