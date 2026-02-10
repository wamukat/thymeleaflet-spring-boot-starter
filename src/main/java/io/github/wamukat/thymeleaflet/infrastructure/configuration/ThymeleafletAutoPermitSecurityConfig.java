package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Optional Spring Security integration for quick adoption.
 *
 * <p>When {@code thymeleaflet.security.auto-permit=true}, this registers
 * a small filter chain that permits only {@code /thymeleaflet/**}.</p>
 */
@Configuration
@ConditionalOnClass(name = {
    "org.springframework.security.web.SecurityFilterChain",
    "org.springframework.security.config.annotation.web.builders.HttpSecurity"
})
@ConditionalOnProperty(name = "thymeleaflet.security.auto-permit", havingValue = "true")
public class ThymeleafletAutoPermitSecurityConfig {

    private final String basePath;

    public ThymeleafletAutoPermitSecurityConfig(ResolvedStorybookConfig resolvedStorybookConfig) {
        this.basePath = sanitizeBasePath(resolvedStorybookConfig.getBasePath());
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 50)
    public SecurityFilterChain thymeleafletAutoPermitSecurityFilterChain(HttpSecurity http) throws Exception {
        String basePathPattern = basePath + "/**";
        http
            .securityMatcher(basePathPattern)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    private static String sanitizeBasePath(String basePath) {
        String trimmed = basePath.trim();
        if (trimmed.isEmpty() || "/".equals(trimmed)) {
            return "";
        }
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
