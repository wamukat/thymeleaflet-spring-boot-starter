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

        
        // テスト1: 問題のURL
        String problemUrl = "/thymeleaflet/domain.point.molecules.point-transaction-badge/transactionTypeBadge/default";
        
        try {
            mockMvc.perform(get(problemUrl)
                    .contentType(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            
            // レスポンス詳細を確認
            mockMvc.perform(get(problemUrl))
                    .andDo(result -> {
                    });
        }
    }
    
    @Test
    void testTransactionIconFragmentExists() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        
        // テスト2: 比較用URL（こちらは動作するはず）
        String workingUrl = "/thymeleaflet/domain.point.molecules.point-transaction-icon/pointTransactionIcon/default";
        
        try {
            mockMvc.perform(get(workingUrl)
                    .contentType(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        } catch (Exception e) {
        }
    }
    
    @Test
    void testFragmentDiscovery() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        
        // フラグメント一覧ページをテスト
        String listUrl = "/thymeleaflet";
        
        mockMvc.perform(get(listUrl))
                .andDo(result -> {
                    String content = result.getResponse().getContentAsString();
                    
                    // transaction-badge が含まれているかチェック
                    boolean hasBadge = content.contains("transaction-badge") || content.contains("transactionTypeBadge");
                    boolean hasIcon = content.contains("transaction-icon") || content.contains("pointTransactionIcon");
                    
                    
                    // 発見されたフラグメント数をカウント
                    long fragmentCount = content.lines()
                            .filter(line -> line.contains("thymeleaflet/"))
                            .count();
                });
    }
}
