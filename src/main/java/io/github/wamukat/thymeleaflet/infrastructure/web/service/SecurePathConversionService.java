package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.domain.model.SecureTemplatePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.Optional;

/**
 * セキュアパス変換専用サービス
 * 
 * 責務: セキュリティパス変換とエラーハンドリング
 * StoryPreviewService SRP違反解決のための責務分離
 */
@Component
public class SecurePathConversionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurePathConversionService.class);
    
    /**
     * セキュアパス変換処理
     * 
     * 共通的なセキュリティパス変換とエラーハンドリング
     * 
     * @param templatePath エンコード済みテンプレートパス
     * @param model Spring MVCモデル
     * @return 変換結果
     */
    public SecurityConversionResult convertSecurePath(String templatePath, Model model) {
        try {
            logger.info("[PATH_CONVERSION] Before conversion: {}", templatePath);
            SecureTemplatePath secureTemplatePath = SecureTemplatePath.of(templatePath);
            String fullTemplatePath = secureTemplatePath.forFilePath();
            logger.info("[PATH_CONVERSION] After conversion: {}", fullTemplatePath);
            return SecurityConversionResult.success(fullTemplatePath);
        } catch (SecurityException e) {
            logger.error("Security violation in template path conversion: {}", e.getMessage());
            model.addAttribute("error", "不正なテンプレートパスです: " + e.getMessage());
            return SecurityConversionResult.failure("thymeleaflet/fragments/error-display :: error(type='danger')");
        }
    }
    
    /**
     * セキュリティ変換処理結果
     */
    public static class SecurityConversionResult {
        private final boolean succeeded;
        private final Optional<String> fullTemplatePath;
        private final Optional<String> templateReference;
        
        private SecurityConversionResult(
            boolean succeeded,
            Optional<String> fullTemplatePath,
            Optional<String> templateReference
        ) {
            this.succeeded = succeeded;
            this.fullTemplatePath = fullTemplatePath;
            this.templateReference = templateReference;
        }
        
        public static SecurityConversionResult success(String fullTemplatePath) {
            return new SecurityConversionResult(true, Optional.of(fullTemplatePath), Optional.empty());
        }
        
        public static SecurityConversionResult failure(String templateReference) {
            return new SecurityConversionResult(false, Optional.empty(), Optional.of(templateReference));
        }
        
        public boolean succeeded() { return succeeded; }
        public Optional<String> fullTemplatePath() { return fullTemplatePath; }
        public Optional<String> templateReference() { return templateReference; }
    }
}
