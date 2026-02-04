package io.github.wamukat.thymeleaflet.domain.service;

import java.util.List;

/**
 * セキュリティ検証例外 - Domain Exception
 * 
 * パスセキュリティ検証でセキュリティ違反が検出された際にスローされる例外
 * Clean Architectureの原則に従い、フレームワークに依存しない純粋なドメイン例外
 */
public class SecurityValidationException extends RuntimeException {

    private final SecurityViolationType violationType;
    private final String detectedValue;

    /**
     * セキュリティ検証例外を作成
     * 
     * @param message セキュリティ違反メッセージ
     * @param violationType 違反の種別
     */
    public SecurityValidationException(String message, SecurityViolationType violationType) {
        super(message);
        this.violationType = violationType;
        this.detectedValue = null;
    }

    /**
     * セキュリティ検証例外を作成（複数違反、原因例外付き）
     * 
     * @param message セキュリティ違反メッセージ
     * @param violations 違反の種別リスト
     * @param cause 原因例外
     */
    public SecurityValidationException(String message, List<SecurityViolationType> violations, Throwable cause) {
        super(message, cause);

        if (!violations.isEmpty()) {
            this.violationType = violations.get(0); // 主要な違反を設定
        } else {
            this.violationType = SecurityViolationType.GENERAL_SECURITY_VIOLATION;
        }
        
        this.detectedValue = null;
    }

    @Override
    public String toString() {
        return "SecurityValidationException{" +
                "violationType=" + violationType +
                ", detectedValue='" + detectedValue + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}