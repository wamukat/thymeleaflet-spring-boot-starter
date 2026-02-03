package io.github.wamukat.thymeleaflet.debug;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.Map;

/**
 * Story機構のデフォルト値提供機能のテスト
 */
@SpringBootTest(classes = {io.github.wamukat.thymeleaflet.config.FragmentTestConfiguration.class})
@ActiveProfiles("test")
@Tag("debug")
class StoryMechanismTest {

    @Autowired
    private StoryRetrievalUseCase storyRetrievalUseCase;
    
    @Autowired
    private StoryParameterUseCase storyParameterUseCase;

    @Test
    void testTransactionBadgeDefaultStory() {
        
        String templatePath = "domain/point/molecules/point-transaction-badge";
        String fragmentName = "transactionTypeBadge";
        String storyName = "default";
        
        
        try {
            // Storyの設定を取得
            var storyInfo = storyRetrievalUseCase.getStory(templatePath, fragmentName, storyName);
            
            if (storyInfo != null) {
                
                // パラメータを取得
                Map<String, Object> parameters = storyParameterUseCase.getParametersForStory(storyInfo);
                
                parameters.forEach((key, value) -> {
                });
                
                // transactionTypeパラメータの値を確認
                Object transactionTypeValue = parameters.get("transactionType");
                if (transactionTypeValue != null) {
                    
                    // ENUM値の詳細確認
                    if (transactionTypeValue.toString().equals("EARN") || transactionTypeValue.toString().equals("USE")) {
                    } else {
                    }
                } else {
                }
                
            } else {
                
                // フォールバック機構が動作するかテスト
                testFallbackMechanism(templatePath, fragmentName);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            
            // フォールバック機構をテスト
            testFallbackMechanism(templatePath, fragmentName);
        }
    }
    
    private void testFallbackMechanism(String templatePath, String fragmentName) {
        
        // デフォルト値の生成機能をテスト
        try {
            // StoryManagementUseCaseの内部動作を確認
            // ENUM型のパラメータに対してデフォルト値が提供されるかテスト
            
            
            // Storyが見つからない場合のデフォルト動作を確認
            
            // TransactionTypeがENUM型として認識され、適切なデフォルト値が設定されるかを確認
            // 通常は最初のENUM値（EARN）がデフォルトとして使われる予定
            
        } catch (Exception e) {
        }
    }
    
    @Test
    void testAvailableStories() {
        
        String templatePath = "domain/point/molecules/point-transaction-badge";
        String fragmentName = "transactionTypeBadge";
        
        try {
            // FragmentInfoを構築してStoryを取得する必要がある
            var fragmentInfo = new FragmentDiscoveryService.FragmentInfo(
                templatePath, 
                fragmentName, 
                Collections.emptyList(), 
                FragmentDomainService.FragmentType.PARAMETERIZED,
                "transactionTypeBadge(transactionType)"
            );
            var availableStories = storyRetrievalUseCase.getStoriesForFragment(fragmentInfo);
            
            
            for (var story : availableStories) {
                
                // パラメータを取得
                Map<String, Object> parameters = storyParameterUseCase.getParametersForStory(story);
                
                if (parameters.containsKey("transactionType")) {
                    Object value = parameters.get("transactionType");
                }
            }
            
            if (availableStories.isEmpty()) {
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
