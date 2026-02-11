package io.github.wamukat.thymeleaflet.infrastructure.web.controller;

import io.github.wamukat.thymeleaflet.TestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
