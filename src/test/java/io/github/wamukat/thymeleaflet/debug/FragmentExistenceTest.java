package io.github.wamukat.thymeleaflet.debug;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * フラグメント存在確認テスト
 */
@SpringBootTest(classes = {io.github.wamukat.thymeleaflet.config.FragmentTestConfiguration.class})
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Tag("debug")
class FragmentExistenceTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void testTransactionBadgeFragmentExists() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        System.out.println("=== Testing transaction badge fragment existence ===");
        
        // テスト1: 問題のURL
        String problemUrl = "/thymeleaflet/domain.point.molecules.point-transaction-badge/transactionTypeBadge/default";
        System.out.println("Testing URL: " + problemUrl);
        
        try {
            mockMvc.perform(get(problemUrl)
                    .contentType(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
            System.out.println("✅ Transaction badge fragment found and accessible");
        } catch (Exception e) {
            System.out.println("❌ Transaction badge fragment failed: " + e.getMessage());
            
            // レスポンス詳細を確認
            mockMvc.perform(get(problemUrl))
                    .andDo(result -> {
                        System.out.println("Response status: " + result.getResponse().getStatus());
                        System.out.println("Response content type: " + result.getResponse().getContentType());
                        System.out.println("Response content: " + result.getResponse().getContentAsString());
                    });
        }
    }
    
    @Test
    void testTransactionIconFragmentExists() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        System.out.println("=== Testing transaction icon fragment existence ===");
        
        // テスト2: 比較用URL（こちらは動作するはず）
        String workingUrl = "/thymeleaflet/domain.point.molecules.point-transaction-icon/pointTransactionIcon/default";
        System.out.println("Testing URL: " + workingUrl);
        
        try {
            mockMvc.perform(get(workingUrl)
                    .contentType(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
            System.out.println("✅ Transaction icon fragment found and accessible");
        } catch (Exception e) {
            System.out.println("❌ Transaction icon fragment failed: " + e.getMessage());
        }
    }
    
    @Test
    void testFragmentDiscovery() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        System.out.println("=== Testing fragment discovery ===");
        
        // フラグメント一覧ページをテスト
        String listUrl = "/thymeleaflet";
        System.out.println("Testing fragment list URL: " + listUrl);
        
        mockMvc.perform(get(listUrl))
                .andDo(result -> {
                    String content = result.getResponse().getContentAsString();
                    System.out.println("Fragment list response length: " + content.length());
                    
                    // transaction-badge が含まれているかチェック
                    boolean hasBadge = content.contains("transaction-badge") || content.contains("transactionTypeBadge");
                    boolean hasIcon = content.contains("transaction-icon") || content.contains("pointTransactionIcon");
                    
                    System.out.println("Contains transaction-badge: " + hasBadge);
                    System.out.println("Contains transaction-icon: " + hasIcon);
                    
                    // 発見されたフラグメント数をカウント
                    long fragmentCount = content.lines()
                            .filter(line -> line.contains("thymeleaflet/"))
                            .count();
                    System.out.println("Total fragment links found: " + fragmentCount);
                });
    }
}
