package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import io.github.wamukat.thymeleaflet.TestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestApplication.class, StorybookI18nIntegrationTest.CustomI18nConfig.class})
@AutoConfigureWebMvc
@ActiveProfiles("test")
class StorybookI18nIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    @DisplayName("Custom MessageSourceでもThymeleafletのメッセージが解決される")
    void shouldResolveThymeleafletMessagesWithCustomMessageSource() throws Exception {
        String body = mockMvc.perform(get("/thymeleaflet"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(body.contains("???thymeleaflet."), "Thymeleafletのメッセージキーが未解決にならないこと");
        assertTrue(body.contains("Select a fragment"), "Thymeleafletの英語メッセージが表示されること");

        MessageSource messageSource = applicationContext.getBean("messageSource", MessageSource.class);
        assertTrue(messageSource instanceof AbstractMessageSource, "messageSourceはAbstractMessageSourceであること");
        AbstractMessageSource abstractMessageSource = (AbstractMessageSource) messageSource;
        assertNotNull(abstractMessageSource.getParentMessageSource(), "Thymeleafletメッセージソースが親として設定されること");
    }

    @Test
    @DisplayName("LocaleChangeInterceptor競合が起きない")
    void shouldUseCustomLocaleChangeInterceptorWithoutConflicts() {
        Map<String, LocaleChangeInterceptor> beans = applicationContext.getBeansOfType(LocaleChangeInterceptor.class);
        assertEquals(1, beans.size(), "LocaleChangeInterceptorが複数登録されないこと");
        assertTrue(beans.containsKey("customLocaleChangeInterceptor"), "カスタムインターセプターが使用されること");
    }

    @Configuration
    static class CustomI18nConfig {
        @Bean(name = "messageSource")
        public MessageSource messageSource() {
            ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
            messageSource.setBasename("sample/messages");
            messageSource.setDefaultEncoding("UTF-8");
            return messageSource;
        }

        @Bean
        public LocaleChangeInterceptor customLocaleChangeInterceptor() {
            LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
            interceptor.setParamName("lang");
            return interceptor;
        }
    }
}
