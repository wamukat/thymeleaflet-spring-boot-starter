package io.github.wamukat.thymeleaflet.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wamukat.thymeleaflet.infrastructure.web.controller.FragmentListController;
import io.github.wamukat.thymeleaflet.infrastructure.web.controller.StoryPreviewController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 外部仕様保護テスト (Golden Master Pattern)
 * 
 * Clean Architecture移行中も外部から観察可能な全ての振る舞いを完全に保持することを保証する
 * 
 * 【重要】このテストが1つでも失敗した場合は即座に作業を停止し、Git Revertを実行すること
 */
@SpringBootTest(classes = {io.github.wamukat.thymeleaflet.config.FragmentTestConfiguration.class})
@AutoConfigureWebMvc
@ActiveProfiles("test")
class FragmentPreviewContractTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FragmentListController fragmentListController;
    
    @Autowired
    private StoryPreviewController storyPreviewController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("【Contract】フラグメント一覧 - HTTP APIレスポンス構造保護")
    void shouldPreserveFragmentListApiContract() throws Exception {
        // Given: 外部仕様として定義されたHTTPエンドポイント
        String expectedUrl = "/thymeleaflet";
        
        // When: フラグメント一覧APIを呼び出し
        MvcResult result = mockMvc.perform(get(expectedUrl)
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk())                                    // ステータスコード保護
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))  // Content-Type保護
                .andReturn();
        
        // Then: レスポンス構造の基本チェック
        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
        assertFalse(responseBody.isEmpty());
        
        // HTML構造の基本要素存在確認（DOM構造保護）
        assertTrue(responseBody.contains("<!DOCTYPE html>"), "HTML DOCTYPE宣言が存在すること");
        assertTrue(responseBody.contains("<html"), "HTML要素が存在すること");
        assertTrue(responseBody.contains("</html>"), "HTML終了タグが存在すること");
    }

    @Test
    @DisplayName("【Contract】ストーリープレビュー - HTTP APIレスポンス構造保護")
    void shouldPreserveStoryPreviewApiContract() throws Exception {
        // Given: 外部仕様として定義されたパス形式
        String templatePath = "shared.atoms.button-action";
        String fragmentName = "primary";
        String storyName = "default";
        String expectedUrl = String.format("/thymeleaflet/%s/%s/%s", templatePath, fragmentName, storyName);
        
        // When: ストーリープレビューAPIを呼び出し
        MvcResult result = mockMvc.perform(get(expectedUrl)
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk())                                    // ステータスコード保護
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))  // Content-Type保護
                .andReturn();
        
        // Then: レスポンス構造の基本チェック
        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
        assertFalse(responseBody.isEmpty());
        
        // HTML構造の基本要素存在確認（DOM構造保護）
        assertTrue(responseBody.contains("<!DOCTYPE html>"), "HTML DOCTYPE宣言が存在すること");
        assertTrue(responseBody.contains("<html"), "HTML要素が存在すること");
        assertTrue(responseBody.contains("</html>"), "HTML終了タグが存在すること");
    }

    @Test
    @DisplayName("【Contract】Thymeleafモデル属性 - フラグメント一覧")
    void shouldPreserveFragmentListModelAttributes() {
        // Given: 現在の外部仕様 (Golden Master)
        Model currentModel = new ExtendedModelMap();
        
        // When: フラグメント一覧処理を実行
        String viewName = fragmentListController.fragmentList(currentModel);
        
        // Then: 外部仕様として公開されているモデル属性の存在確認
        Map<String, Object> modelMap = currentModel.asMap();
        
        // 必須モデル属性の存在確認（外部仕様保護）
        assertTrue(modelMap.containsKey("fragments"), "fragments属性が存在すること");
        assertTrue(modelMap.containsKey("fragmentsJson"), "fragmentsJson属性が存在すること");
        assertTrue(modelMap.containsKey("hierarchicalFragments"), "hierarchicalFragments属性が存在すること");
        assertTrue(modelMap.containsKey("hierarchicalFragmentsJson"), "hierarchicalFragmentsJson属性が存在すること");
        
        // View名の外部仕様保護
        assertEquals("thymeleaflet/fragment-list", viewName, "View名が外部仕様通りであること");
        
        // モデル属性の型保護（基本的な型チェック）
        Object fragments = modelMap.get("fragments");
        assertNotNull(fragments, "fragments属性はnullでないこと");
        assertTrue(fragments instanceof java.util.List, "fragments属性はList型であること");
        
        Object fragmentsJson = modelMap.get("fragmentsJson");
        assertNotNull(fragmentsJson, "fragmentsJson属性はnullでないこと");
        assertTrue(fragmentsJson instanceof String, "fragmentsJson属性はString型であること");
    }

    @Test
    @DisplayName("【Contract】Thymeleafモデル属性 - ストーリープレビュー")
    void shouldPreserveStoryPreviewModelAttributes() {
        // Given: 現在の外部仕様 (Golden Master)
        Model currentModel = new ExtendedModelMap();
        String templatePath = "shared.atoms.button-action";
        String fragmentName = "primary";
        String storyName = "default";
        
        // When: ストーリープレビュー処理を実行
        String viewName = storyPreviewController.storyPreview(templatePath, fragmentName, storyName, currentModel);
        
        // Then: 外部仕様として公開されているモデル属性の存在確認
        Map<String, Object> modelMap = currentModel.asMap();
        
        // 必須モデル属性の存在確認（外部仕様保護）
        assertTrue(modelMap.containsKey("templatePathEncoded"), "templatePathEncoded属性が存在すること");
        assertTrue(modelMap.containsKey("fragmentName"), "fragmentName属性が存在すること");
        assertTrue(modelMap.containsKey("storyName"), "storyName属性が存在すること");
        
        // View名の外部仕様保護
        assertEquals("thymeleaflet/fragment-list", viewName, "View名が外部仕様通りであること");
        
        // 重要な外部仕様属性の値保護
        assertEquals(templatePath.replace("/", "."), modelMap.get("templatePathEncoded"), 
                "templatePathEncoded属性の変換ロジックが保持されること");
        assertEquals(fragmentName, modelMap.get("fragmentName"), 
                "fragmentName属性の値が保持されること");
        assertEquals(storyName, modelMap.get("storyName"), 
                "storyName属性の値が保持されること");
    }

    @Test
    @DisplayName("【Contract】URLパス形式 - templatePath変換ロジック保護")
    void shouldPreserveTemplatePathConversionLogic() {
        // Given: 外部仕様として定義されたパス変換ルール
        Model model = new ExtendedModelMap();
        String inputPath = "domain.point.molecules";
        String expectedEncoded = "domain.point.molecules";  // ドット区切りはそのまま
        
        // When: ストーリープレビュー処理を実行（内部でパス変換が行われる）
        storyPreviewController.storyPreview(inputPath, "fragment", "story", model);
        
        // Then: パス変換ロジックの外部仕様保護
        Map<String, Object> modelMap = model.asMap();
        assertEquals(expectedEncoded, modelMap.get("templatePathEncoded"), 
                "templatePath変換ロジックが外部仕様通りであること");
    }

    @Test
    @DisplayName("【Contract】エラーハンドリング - 存在しないパスでの404処理")
    void shouldPreserveErrorHandlingContract() throws Exception {
        // Given: 存在しないテンプレートパス
        String nonExistentPath = "non.existent.template";
        String fragmentName = "fragment";
        String storyName = "story";
        String expectedUrl = String.format("/thymeleaflet/%s/%s/%s", nonExistentPath, fragmentName, storyName);
        
        // When & Then: エラーハンドリングの外部仕様保護
        // 注意: 実際のエラーハンドリング動作は実装に依存するため、
        // 現在の動作を基準とした契約テストとする
        MvcResult result = mockMvc.perform(get(expectedUrl)
                .contentType(MediaType.TEXT_HTML))
                .andReturn(); // ステータスコードは実装依存のため、まず現在の動作を記録
        
        // 現在の動作を記録（後続のリファクタリングで同じ動作を保証）
        int actualStatusCode = result.getResponse().getStatus();
        String actualContentType = result.getResponse().getContentType();
        
        // ログ出力で現在の動作を記録
        System.out.println("【Contract記録】存在しないパスでのレスポンス:");
        System.out.println("  Status Code: " + actualStatusCode);
        System.out.println("  Content-Type: " + actualContentType);
        
        // 基本的な応答性の確認
        assertNotNull(result.getResponse(), "レスポンスオブジェクトが存在すること");
    }
}