package io.github.wamukat.thymeleaflet.domain.service;

/**
 * セキュリティ違反種別 - Domain Enum
 * 
 * パスセキュリティ検証で検出される違反の種類を定義
 */
public enum SecurityViolationType {

    /**
     * Path Traversal攻撃（../、..\ 等の検出）
     */
    PATH_TRAVERSAL("Path traversal attack detected"),

    /**
     * 無効なパス形式（NULL、空文字列、不正形式等）
     */
    INVALID_FORMAT("Invalid path format"),

    /**
     * 禁止文字の使用（スラッシュ、バックスラッシュ等）
     */
    FORBIDDEN_CHARACTERS("Forbidden characters detected"),

    /**
     * URLエンコード攻撃
     */
    URL_ENCODING_ATTACK("URL encoding attack detected"),

    /**
     * Unicode正規化攻撃
     */
    UNICODE_NORMALIZATION_ATTACK("Unicode normalization attack detected"),

    /**
     * 過度な長さ制限違反
     */
    EXCESSIVE_LENGTH("Path length exceeds maximum allowed"),

    /**
     * 禁止セグメント使用
     */
    FORBIDDEN_SEGMENT("Forbidden path segment detected"),

    /**
     * NULL または空文字列
     */
    NULL_OR_EMPTY("Path cannot be null or empty"),

    /**
     * パスが長すぎる
     */
    PATH_TOO_LONG("Path length exceeds maximum allowed"),

    /**
     * Unicode攻撃
     */
    UNICODE_ATTACK("Unicode attack detected"),

    /**
     * パストラバーサル攻撃の試行
     */
    PATH_TRAVERSAL_ATTEMPT("Path traversal attempt detected"),

    /**
     * 禁止されたパスセグメント
     */
    FORBIDDEN_PATH_SEGMENT("Forbidden path segment detected"),

    /**
     * 検証処理中のエラー
     */
    VALIDATION_ERROR("Validation error occurred"),

    /**
     * その他の一般的なセキュリティ違反
     */
    GENERAL_SECURITY_VIOLATION("Security violation detected");

    private final String defaultMessage;

    SecurityViolationType(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

}