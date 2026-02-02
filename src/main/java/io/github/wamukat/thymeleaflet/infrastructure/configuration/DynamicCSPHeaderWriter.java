package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.header.HeaderWriter;

/**
 * 動的CSPヘッダー設定
 * 
 * Content Security Policyを動的に設定
 */
public class DynamicCSPHeaderWriter implements HeaderWriter {
    
    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        String cspValue = buildStandardCSP();
        response.setHeader("Content-Security-Policy", cspValue);
        response.setHeader("X-Frame-Options", "DENY");
    }
    
    /**
     * 標準的な厳格CSP
     */
    private String buildStandardCSP() {
        return String.join("; ", 
            "default-src 'self'",
            "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://unpkg.com https://cdn.jsdelivr.net",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "img-src 'self' data: https:",
            "connect-src 'self'",
            "frame-src 'none'",
            "frame-ancestors 'none'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "upgrade-insecure-requests"
        );
    }
}
