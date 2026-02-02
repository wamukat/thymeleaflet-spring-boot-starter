package io.github.wamukat.thymeleaflet.application.port.inbound.coordination;

import org.springframework.ui.Model;

/**
 * ストーリーページ協調ユースケース
 * 
 * 複数のUseCaseを協調させてストーリープレビューページのセットアップを行う
 * Controller肥大化問題の解決のため、Application層で複数UseCase協調処理を実施
 */
public interface StoryPageCoordinationUseCase {
    
    /**
     * ストーリーページ全体のセットアップを協調実行
     * 
     * @param request ストーリーページ要求情報
     * @return 統合されたページセットアップ結果
     */
    StoryPageResult coordinateStoryPageSetup(StoryPageRequest request);
    
    /**
     * ストーリーページ要求情報
     */
    record StoryPageRequest(
        String fullTemplatePath,
        String fragmentName, 
        String storyName,
        Model model
    ) {}
    
    /**
     * ストーリーページセットアップ結果
     */
    record StoryPageResult(
        boolean succeeded,
        String errorMessage
    ) {
        public static StoryPageResult success() {
            return new StoryPageResult(true, null);
        }
        
        public static StoryPageResult failure(String errorMessage) {
            return new StoryPageResult(false, errorMessage);
        }
    }
}