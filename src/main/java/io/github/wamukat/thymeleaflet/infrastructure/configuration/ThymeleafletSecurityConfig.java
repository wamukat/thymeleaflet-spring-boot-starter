package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

/**
 * Thymeleaflet包括的セキュリティ設定
 * 
 * CSRF保護、セキュリティヘッダー、セッション管理を統合設定
 * Expert Review対応: 多層防御セキュリティアーキテクチャ
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "thymeleaflet.security.enabled", havingValue = "true", matchIfMissing = true)
public class ThymeleafletSecurityConfig {
    
    /**
     * Thymeleaflet専用セキュリティフィルターチェーン
     */
    @Bean
    public SecurityFilterChain thymeleafletSecurityFilter(HttpSecurity http) throws Exception {
        http
            // Thymeleafletエンドポイントのみに適用
            .securityMatcher("/thymeleaflet/**")
            
            // 認可設定
            .authorizeHttpRequests(authz -> authz
                // 管理者機能は ADMIN ロール必須
                .requestMatchers("/thymeleaflet/actuator/security/**").hasRole("ADMIN")
                .requestMatchers("/thymeleaflet/admin/**").hasRole("ADMIN")
                
                // 開発環境用プレビューは認証不要（開発時のみ）
                .requestMatchers("/thymeleaflet/**").permitAll()
                
                .anyRequest().authenticated()
            )
            
            // CSRF保護設定 (Critical Issue対応)
            .csrf(csrf -> csrf
                // Cookie ベース CSRF トークン
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                
                // XOR CSRF ハンドラー（セキュリティ強化）
                .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
                
                // 除外対象（API エンドポイントなど）
                .ignoringRequestMatchers(
                    "/thymeleaflet/main-content",  // メインコンテンツ遅延読み込み
                    "/thymeleaflet/*/render",      // HTMX プレビュー
                    "/thymeleaflet/*/usage",       // 使用例表示
                    "/thymeleaflet/*/content"      // コンテンツ片取得
                )
            )
            
            // セキュリティヘッダー設定 (Expert Review対応)
            .headers(headers -> headers
                // フレーム制御: DynamicCSPHeaderWriterで動的制御するため無効化
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                
                // コンテンツタイプ固定: MIME スニッフィング攻撃防止
                .contentTypeOptions(Customizer.withDefaults())
                
                // HTTPS 強制 (Production)
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)  // 1年間
                    .includeSubDomains(true)
                    .preload(true)
                )
                
                // リファラーポリシー、権限ポリシーはヘッダー設定として追加
                .addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", "strict-origin-when-cross-origin"))
                .addHeaderWriter(new StaticHeadersWriter(
                    "Permissions-Policy", 
                    "geolocation=(), microphone=(), camera=(), fullscreen=(self), payment=()"
                ))
                
                // 動的CSP設定
                .addHeaderWriter(new DynamicCSPHeaderWriter())
            )
            
            // セッション管理
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().migrateSession()  // セッション固定攻撃防止
                .invalidSessionUrl("/thymeleaflet")
            )
            
            // 例外ハンドリング
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedPage("/thymeleaflet/security-error")
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendRedirect("/thymeleaflet/login");
                })
            );
        
        return http.build();
    }
    
    /**
     * セッション管理レジストリ
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
    
}
