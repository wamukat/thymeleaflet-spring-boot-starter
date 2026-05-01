package io.github.wamukat.thymeleaflet.infrastructure.web.controller;

import io.github.wamukat.thymeleaflet.TestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = TestApplication.class,
    properties = "spring.main.allow-bean-definition-overriding=true"
)
@AutoConfigureWebMvc
@ActiveProfiles("test")
class ThymeleafletRenderingExceptionHandlerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build();
    }

    @Test
    @DisplayName("th:replace に通常文字列が渡された場合はエラーUIへフォールバックする")
    void shouldFallbackToErrorFragmentOnInvalidFragmentExpression() throws Exception {
        String body = mockMvc.perform(get("/thymeleaflet/test.unsafe-fragment/unsafe/default/render")
                .header("Accept-Language", "en"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertTrue(body.contains("Preview error"),
            "テンプレート例外時にプレビューエラー表示へフォールバックすること");
        assertTrue(
            body.contains("th:replace/th:insert")
                || body.contains("thymeleaflet.error.message.invalidFragmentExpression"),
            "原因ヒント付きメッセージが表示されること");
        assertFalse(body.contains("Whitelabel Error Page"),
            "ホストアプリのデフォルトエラーページに遷移しないこと");
    }

    @Test
    @DisplayName("JavaDoc @param の java.time 型に基づいて story parameters を変換して描画する")
    void shouldRenderJavaTimeParameterValuesFromStoryYaml() throws Exception {
        String body = mockMvc.perform(get("/thymeleaflet/test.java-time-story/detailHeader/default/render")
                .header("Accept-Language", "en"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertTrue(body.contains("Campaign"), "通常の String パラメータはそのまま描画されること");
        assertTrue(body.contains("2026-04-01 10:00"),
            "LocalDateTime パラメータを #temporals.format で描画できること");
        assertFalse(body.contains("Preview error"),
            "java.time 変換によりプレビューエラーにならないこと");
    }

    @Test
    @DisplayName("JavaDoc @model の [] パスに基づいて story model 内の java.time 値を変換して描画する")
    void shouldRenderJavaTimeModelListValuesFromStoryYaml() throws Exception {
        String body = mockMvc.perform(get("/thymeleaflet/test.java-time-story/noticeList/default/render")
                .header("Accept-Language", "en"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertTrue(body.contains("Notice 1"), "model の通常フィールドはそのまま描画されること");
        assertTrue(body.contains("2024-06-01 10:00"),
            "list 内の LocalDateTime フィールドを #temporals.format で描画できること");
        assertTrue(body.contains("2024-06-02 11:30"),
            "[] パスが list の全要素に適用されること");
        assertFalse(body.contains("Preview error"),
            "java.time 変換によりプレビューエラーにならないこと");
    }

    @Test
    @DisplayName("パラメータなしフラグメント参照の name() 形式をプレビューで解決できる")
    void shouldRenderNoArgFragmentReferenceWithEmptyParentheses() throws Exception {
        String body = mockMvc.perform(get("/thymeleaflet/test.no-arg-fragment-reference/shell/default/render")
                .header("Accept-Language", "en"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertTrue(body.contains("Topbar OK"),
            "別テンプレートの no-arg fragment を name() 形式で参照しても描画できること");
        assertFalse(body.contains("Preview error"),
            "no-arg fragment 参照によりプレビューエラーにならないこと");
    }

    @Test
    @DisplayName("Map no-arg メソッドが未解決でも /render は継続し警告ヘッダーを返す")
    void shouldRenderWithWarningsForUnresolvedMapNoArgMethods() throws Exception {
        var mvcResult = mockMvc.perform(get("/thymeleaflet/test.map-noarg-warning/methodWarning/default/render")
                .header("Accept-Language", "en"))
            .andExpect(status().isOk())
            .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        String warningsHeader = mvcResult.getResponse().getHeader("X-Thymeleaflet-Preview-Warnings");

        assertTrue(warningsHeader != null && !warningsHeader.isBlank(),
            "未解決 no-arg メソッド時は警告ヘッダーが設定されること");

        String decodedWarnings = new String(
            Base64.getUrlDecoder().decode(warningsHeader),
            StandardCharsets.UTF_8
        );
        assertTrue(
            decodedWarnings.contains("hasPrev()") || decodedWarnings.contains("nextPage()"),
            "警告内容に未解決メソッド名が含まれること"
        );
        assertTrue(body.contains("no-prev"),
            "未解決の boolean no-arg メソッドは false フォールバックで評価できること");
        assertFalse(body.contains("Whitelabel Error Page"),
            "未解決 no-arg メソッドでもホストアプリのデフォルトエラーページに遷移しないこと");
    }

    @Test
    @DisplayName("fallback ストーリーでは推論 methodReturns を適用して no-arg 警告を抑制する")
    void shouldRenderWithoutWarningsWhenFallbackStoryUsesNoArgMethods() throws Exception {
        var mvcResult = mockMvc.perform(get("/thymeleaflet/test.map-noarg-fallback/fallbackMethodWarning/default/render")
                .header("Accept-Language", "en"))
            .andExpect(status().isOk())
            .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        String warningsHeader = mvcResult.getResponse().getHeader("X-Thymeleaflet-Preview-Warnings");

        assertTrue(body.contains("no-prev"),
            "推論 methodReturns の hasPrev=false が適用されること");
        assertTrue(body.contains("0"),
            "推論 methodReturns の nextPage=0 が適用されること");
        assertTrue(warningsHeader == null || warningsHeader.isBlank(),
            "fallback 推論で解決できる no-arg メソッドは警告を出さないこと");
    }

    @Test
    @DisplayName("methodReturns override で no-arg メソッド戻り値を制御できる")
    void shouldRenderUsingMethodReturnsOverrides() throws Exception {
        String requestBody = """
            {
              "methodReturns": {
                "view": {
                  "pointPage": {
                    "hasPrev": true,
                    "nextPage": 7
                  }
                }
              }
            }
            """;

        var mvcResult = mockMvc.perform(post("/thymeleaflet/test.map-noarg-warning/methodWarning/default/render")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("Accept-Language", "en"))
            .andExpect(status().isOk())
            .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        String warningsHeader = mvcResult.getResponse().getHeader("X-Thymeleaflet-Preview-Warnings");

        assertTrue(body.contains("prev"), "hasPrev=true により Prev 表示が有効になること");
        assertTrue(body.contains("7"), "nextPage=7 が描画されること");
        assertTrue(warningsHeader == null || warningsHeader.isBlank(),
            "衝突・未解決がなければ警告ヘッダーが空であること");
    }

    @Test
    @DisplayName("model と methodReturns の衝突時は model を優先し警告を返す")
    void shouldWarnAndKeepModelValueOnMethodReturnConflict() throws Exception {
        String requestBody = """
            {
              "model": {
                "view": {
                  "pointPage": {
                    "hasPrev": true
                  }
                }
              },
              "methodReturns": {
                "view": {
                  "pointPage": {
                    "hasPrev": false,
                    "nextPage": 8
                  }
                }
              }
            }
            """;

        var mvcResult = mockMvc.perform(post("/thymeleaflet/test.map-noarg-warning/methodWarning/default/render")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("Accept-Language", "en"))
            .andExpect(status().isOk())
            .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        String warningsHeader = mvcResult.getResponse().getHeader("X-Thymeleaflet-Preview-Warnings");

        assertTrue(warningsHeader != null && !warningsHeader.isBlank(),
            "衝突時は警告ヘッダーが設定されること");
        String decodedWarnings = new String(
            Base64.getUrlDecoder().decode(warningsHeader),
            StandardCharsets.UTF_8
        );
        assertTrue(decodedWarnings.contains("hasPrev"),
            "衝突した methodReturns パスが警告に含まれること");
        assertTrue(body.contains("prev"),
            "model 側 hasPrev=true が優先されること");
        assertTrue(body.contains("8"),
            "衝突していない nextPage は methodReturns から適用されること");
    }
}
