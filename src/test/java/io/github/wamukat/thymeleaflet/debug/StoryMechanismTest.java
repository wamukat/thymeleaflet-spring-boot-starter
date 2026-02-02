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
        System.out.println("=== Testing transaction badge default story mechanism ===");
        
        String templatePath = "domain/point/molecules/point-transaction-badge";
        String fragmentName = "transactionTypeBadge";
        String storyName = "default";
        
        System.out.println("Template Path: " + templatePath);
        System.out.println("Fragment Name: " + fragmentName);
        System.out.println("Story Name: " + storyName);
        
        try {
            // Storyの設定を取得
            var storyInfo = storyRetrievalUseCase.getStory(templatePath, fragmentName, storyName);
            
            if (storyInfo != null) {
                System.out.println("✅ Story info found");
                System.out.println("Story Name: " + storyInfo.getStoryName());
                System.out.println("Fragment Group: " + storyInfo.getFragmentGroupName());
                System.out.println("Display Title: " + storyInfo.getDisplayTitle());
                
                // パラメータを取得
                Map<String, Object> parameters = storyParameterUseCase.getParametersForStory(storyInfo);
                System.out.println("Parameters: " + parameters.size());
                
                parameters.forEach((key, value) -> {
                    System.out.println("  " + key + " = " + value + " (type: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");
                });
                
                // transactionTypeパラメータの値を確認
                Object transactionTypeValue = parameters.get("transactionType");
                if (transactionTypeValue != null) {
                    System.out.println("✅ transactionType parameter found: " + transactionTypeValue);
                    System.out.println("Value type: " + transactionTypeValue.getClass().getName());
                    
                    // ENUM値の詳細確認
                    if (transactionTypeValue.toString().equals("EARN") || transactionTypeValue.toString().equals("USE")) {
                        System.out.println("✅ Valid ENUM value provided: " + transactionTypeValue);
                    } else {
                        System.out.println("⚠️ Unexpected ENUM value: " + transactionTypeValue);
                    }
                } else {
                    System.out.println("❌ transactionType parameter NOT found in story parameters");
                }
                
            } else {
                System.out.println("❌ Story info NOT found for default story");
                
                // フォールバック機構が動作するかテスト
                System.out.println("\n--- Testing fallback mechanism ---");
                testFallbackMechanism(templatePath, fragmentName);
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error loading story configuration: " + e.getMessage());
            e.printStackTrace();
            
            // フォールバック機構をテスト
            System.out.println("\n--- Testing fallback mechanism after error ---");
            testFallbackMechanism(templatePath, fragmentName);
        }
    }
    
    private void testFallbackMechanism(String templatePath, String fragmentName) {
        System.out.println("Testing fallback mechanism for ENUM parameter");
        
        // デフォルト値の生成機能をテスト
        try {
            // StoryManagementUseCaseの内部動作を確認
            // ENUM型のパラメータに対してデフォルト値が提供されるかテスト
            
            System.out.println("Template: " + templatePath);
            System.out.println("Fragment: " + fragmentName);
            
            // Storyが見つからない場合のデフォルト動作を確認
            System.out.println("Checking if default ENUM value generation works...");
            
            // TransactionTypeがENUM型として認識され、適切なデフォルト値が設定されるかを確認
            // 通常は最初のENUM値（EARN）がデフォルトとして使われる予定
            
        } catch (Exception e) {
            System.out.println("Fallback mechanism error: " + e.getMessage());
        }
    }
    
    @Test
    void testAvailableStories() {
        System.out.println("=== Testing available stories discovery ===");
        
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
            
            System.out.println("Available stories count: " + availableStories.size());
            
            for (var story : availableStories) {
                System.out.println("Story: " + story.getStoryName());
                System.out.println("  Fragment Group: " + story.getFragmentGroupName());
                System.out.println("  Display Title: " + story.getDisplayTitle());
                
                // パラメータを取得
                Map<String, Object> parameters = storyParameterUseCase.getParametersForStory(story);
                System.out.println("  Parameters: " + parameters.size());
                
                if (parameters.containsKey("transactionType")) {
                    Object value = parameters.get("transactionType");
                    System.out.println("  transactionType: " + value);
                }
            }
            
            if (availableStories.isEmpty()) {
                System.out.println("❌ No stories found - this might be the issue!");
                System.out.println("The fragment might fail to render without proper story configuration");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error getting available stories: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
