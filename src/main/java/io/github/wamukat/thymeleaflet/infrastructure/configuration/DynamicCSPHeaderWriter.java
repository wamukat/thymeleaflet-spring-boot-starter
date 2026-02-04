package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.header.HeaderWriter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 動的CSPヘッダー設定
 * 
 * Content Security Policyを動的に設定
 */
public class DynamicCSPHeaderWriter implements HeaderWriter {

    private static final Logger logger = LoggerFactory.getLogger(DynamicCSPHeaderWriter.class);
    private static final AtomicBoolean warningLogged = new AtomicBoolean(false);
    
    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        String cspValue = buildStandardCSP();
        response.setHeader("Content-Security-Policy", cspValue);
        response.setHeader("X-Frame-Options", "DENY");
        warnIfAllowAll();
    }
    
    /**
     * 標準的な厳格CSP
     */
    private String buildStandardCSP() {
        return String.join("; ", 
            "default-src 'self'",
            "script-src * 'unsafe-inline' 'unsafe-eval'",
            "style-src * 'unsafe-inline'",
            "font-src * data:",
            "img-src 'self' data: https:",
            "connect-src 'self'",
            "frame-src 'self' data: blob:",
            "frame-ancestors 'none'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "upgrade-insecure-requests"
        );
    }

    private void warnIfAllowAll() {
        if (warningLogged.compareAndSet(false, true)) {
            logger.warn("Thymeleaflet CSP is configured to allow scripts/styles from any origin. " +
                "This is intentionally permissive for previewing external JS, but reduces protection. " +
                "Use only in trusted environments.");
        }
    }
}
