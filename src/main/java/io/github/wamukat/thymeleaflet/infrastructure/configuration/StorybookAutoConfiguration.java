package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Storybook機能の自動設定クラス
 * 
 * Thymeleafフラグメントのストーリーブック機能を提供します。
 * thymeleaflet.enabledプロパティで機能の有効/無効を制御可能です。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ComponentScan(basePackages = "io.github.wamukat.thymeleaflet")
@EnableConfigurationProperties(StorybookProperties.class)
public class StorybookAutoConfiguration {
    
    /**
     * Storybook機能が有効化されたことをログ出力
     */
    public StorybookAutoConfiguration() {
        // 起動時のログ出力用
    }

    /**
     * Thymeleaflet Starterの静的リソースハンドリングを設定
     * 最高優先度で処理されるよう調整
     */
    @Bean
    @Order(0) // 最高優先度
    public WebMvcConfigurer thymeleafletResourceConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // 静的リソース専用パスを限定（コントローラーマッピングと競合を避ける）
                registry.addResourceHandler("/thymeleaflet/css/**")
                        .addResourceLocations("classpath:/META-INF/resources/static/css/");
                registry.addResourceHandler("/thymeleaflet/js/**")
                        .addResourceLocations("classpath:/META-INF/resources/static/js/");
                registry.addResourceHandler("/thymeleaflet/images/**")
                        .addResourceLocations("classpath:/META-INF/resources/static/images/");
                        
                // CSS専用ハンドラー（fallback）（キャッシュなし）
                registry.addResourceHandler("/css/thymeleaflet.css")
                        .addResourceLocations("classpath:/META-INF/resources/static/css/");
            }
        };
    }
}
