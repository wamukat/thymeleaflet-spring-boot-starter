package io.github.wamukat.thymeleaflet.domain.model;

import io.github.wamukat.thymeleaflet.domain.service.SecurityValidationException;
import io.github.wamukat.thymeleaflet.domain.service.SecurityViolationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * セキュアテンプレートパス - Domain Value Object
 * 
 * 【重要】この Value Object は templatePath.replace の散在問題を根本解決します
 * 
 * 現在のコードベースで templatePath.replace が 20+ 箇所に散在している問題を、
 * このValue Objectに変換ロジックを集約することで完全に解決します。
 * 
 * Clean Architectureの原則に従い：
 * - 不変性：一度作成されたら変更不可
 * - セキュリティファースト：作成時にセキュリティ検証を必須実行
 * - 依存性逆転：PathSecurityService interface に依存
 * - フレームワーク非依存：純粋なドメインオブジェクト
 */
public final class SecureTemplatePath {

    private static final Logger logger = LoggerFactory.getLogger(SecureTemplatePath.class);
    
    // === セキュリティ設定（統合版） ===
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern DANGEROUS_UNICODE = Pattern.compile(".*[\uFF0E\u2024\u002F\u2044\uFF0F].*");
    private static final Set<String> FORBIDDEN_SEGMENTS = Set.of(
        "..", "~", "\\", "/", ":", 
        "META-INF", "WEB-INF", "classes", 
        "etc", "var", "tmp", "home", "root"
    );
    private static final int MAX_PATH_LENGTH = 200;
    private static final int MAX_DECODE_ITERATIONS = 5;

    private final String originalPath;  // 元のパス (例: "domain.point.molecules")
    private final String securePath;    // セキュリティ検証済みパス (例: "domain/point/molecules")

    /**
     * プライベートコンストラクタ - 不変Value Object設計
     * 
     * ファクトリメソッドのみからのインスタンス化を強制
     * Clean Architecture: 外部依存を排除した純粋なValue Object
     */
    private SecureTemplatePath(String originalPath, String securePath) {
        this.originalPath = Objects.requireNonNull(originalPath, "Original path cannot be null");
        this.securePath = Objects.requireNonNull(securePath, "Secure path cannot be null");
    }


    /**
     * 信頼できるパスから作成（テスト・内部変換専用）
     * 
     * 【注意】この方法はセキュリティ検証をスキップするため、
     * テストコードや信頼できる内部処理でのみ使用してください
     */
    public static SecureTemplatePath createUnsafe(String trustedPath) {
        String normalizedPath = trustedPath.replace(".", "/");
        return new SecureTemplatePath(trustedPath, normalizedPath);
    }

    /**
     * 既に正規化されたパスから作成（レガシー統合用）
     */
    public static SecureTemplatePath fromNormalizedPath(String normalizedPath) {
        String originalPath = normalizedPath.replace("/", ".");
        return new SecureTemplatePath(originalPath, normalizedPath);
    }

    /**
     * セキュリティ検証付きファクトリメソッド（推奨）
     * 
     * 統合されたセキュリティ検証機能
     * Infrastructure層の複数セキュリティクラスを置き換える統一API
     * 
     * @param rawPath 生のパス文字列
     * @return セキュリティ検証済みのSecureTemplatePath
     * @throws SecurityValidationException セキュリティ違反が検出された場合
     */
    public static SecureTemplatePath of(String rawPath) throws SecurityValidationException {
        String validatedPath = validateSecurityAndConvert(rawPath);
        return new SecureTemplatePath(rawPath, validatedPath);
    }

    /**
     * セキュリティ検証済みパスから作成（Infrastructure層内部専用）
     * 
     * Infrastructure層でセキュリティ検証が完了したパスから
     * 直接作成する際に使用（内部API）
     */
    public static SecureTemplatePath fromValidatedPaths(String originalPath, String securePath) {
        return new SecureTemplatePath(originalPath, securePath);
    }

    /**
     * 統合セキュリティ検証メソッド
     * 統合されたセキュリティ検証・変換機能
     * 
     * @param rawPath 検証対象の生パス
     * @return セキュリティ検証済みパス
     * @throws SecurityValidationException セキュリティ違反検出時
     */
    private static String validateSecurityAndConvert(String rawPath) throws SecurityValidationException {
        logger.debug("Starting security validation and conversion for: {}", rawPath);
        
        // 1. NULL・空文字チェック
        if (rawPath == null || rawPath.trim().isEmpty()) {
            throw new SecurityValidationException("Template path cannot be null or empty", SecurityViolationType.NULL_OR_EMPTY);
        }
        
        // 2. 長さ制限チェック
        if (rawPath.length() > MAX_PATH_LENGTH) {
            logger.warn("Path length exceeds maximum allowed: {} > {}", rawPath.length(), MAX_PATH_LENGTH);
            throw new SecurityValidationException("Template path too long: " + rawPath.length() + " characters", SecurityViolationType.PATH_TOO_LONG);
        }
        
        // 3. フォーマット検証
        if (!VALID_PATTERN.matcher(rawPath).matches()) {
            logger.warn("Invalid template path format detected: {}", rawPath);
            throw new SecurityValidationException("Invalid template path format: " + rawPath, SecurityViolationType.INVALID_FORMAT);
        }
        
        // 4. URLデコード攻撃検証
        String decoded = validateAndDecodeRecursively(rawPath);
        
        // 5. Unicode正規化検証
        validateUnicodeNormalization(decoded);
        
        // 6. パス正規化検証
        String normalized = normalizeAndValidatePath(decoded);
        
        // 7. 禁止セグメント検証
        validateForbiddenSegments(normalized);
        
        // 8. セキュア変換実行 ("." → "/")
        String converted = normalized.replace(".", "/");
        
        logger.info("Secure path conversion completed: {} -> {}", rawPath, converted);
        return converted;
    }
    
    /**
     * 再帰的URLデコードと検証
     */
    private static String validateAndDecodeRecursively(String input) throws SecurityValidationException {
        String decoded = input;
        String previous;
        int iterations = 0;
        
        do {
            previous = decoded;
            try {
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
                iterations++;
                
                logger.trace("URL decode iteration {}: {} -> {}", iterations, previous, decoded);
                
            } catch (Exception e) {
                logger.error("URL decoding failed for input: {}", input, e);
                throw new SecurityValidationException("Invalid URL encoding detected: " + input, SecurityViolationType.URL_ENCODING_ATTACK);
            }
            
            // 無限ループ防止
            if (iterations >= MAX_DECODE_ITERATIONS) {
                logger.error("Excessive URL encoding layers detected: {} (iterations: {})", input, iterations);
                throw new SecurityValidationException("Excessive URL encoding layers detected: " + input, SecurityViolationType.URL_ENCODING_ATTACK);
            }
            
        } while (!decoded.equals(previous));
        
        // デコード変化の検証
        if (!input.equals(decoded)) {
            logger.warn("URL decoding performed: {} -> {} (iterations: {})", input, decoded, iterations);
            
            // 攻撃パターンの検出
            if (iterations > 2) {
                logger.error("Suspicious multiple URL encoding detected: {} (iterations: {})", input, iterations);
                throw new SecurityValidationException("Suspicious URL encoding pattern detected", SecurityViolationType.URL_ENCODING_ATTACK);
            }
        }
        
        return decoded;
    }
    
    /**
     * Unicode正規化攻撃の検証
     */
    private static void validateUnicodeNormalization(String templatePath) throws SecurityValidationException {
        // Unicode正規化
        String normalized = Normalizer.normalize(templatePath, Normalizer.Form.NFC);
        
        // 正規化による変化チェック
        if (!templatePath.equals(normalized)) {
            logger.error("Unicode normalization attack detected: {} -> {}", templatePath, normalized);
            throw new SecurityValidationException("Unicode normalization attack detected: " + templatePath, SecurityViolationType.UNICODE_ATTACK);
        }
        
        // 危険なUnicode文字チェック
        if (DANGEROUS_UNICODE.matcher(templatePath).matches()) {
            logger.error("Dangerous Unicode characters detected: {}", templatePath);
            throw new SecurityValidationException("Dangerous Unicode characters detected: " + templatePath, SecurityViolationType.UNICODE_ATTACK);
        }
    }
    
    /**
     * パス正規化と検証
     */
    private static String normalizeAndValidatePath(String decoded) throws SecurityValidationException {
        try {
            String normalized = Paths.get(decoded).normalize().toString();
            
            // 正規化による変化チェック
            if (!decoded.equals(normalized)) {
                logger.warn("Path normalization performed: {} -> {}", decoded, normalized);
                
                // 攻撃の兆候として詳細ログ
                if (normalized.contains("..")) {
                    logger.error("Path traversal attempt detected after normalization: {} -> {}", decoded, normalized);
                    throw new SecurityValidationException("Path traversal attempt detected: " + decoded, SecurityViolationType.PATH_TRAVERSAL_ATTEMPT);
                }
            }
            
            return normalized;
            
        } catch (Exception e) {
            logger.error("Path normalization failed for: {}", decoded, e);
            throw new SecurityValidationException("Invalid path format: " + decoded, SecurityViolationType.INVALID_FORMAT);
        }
    }
    
    /**
     * 禁止セグメントの検証
     */
    private static void validateForbiddenSegments(String path) throws SecurityValidationException {
        for (String forbiddenSegment : FORBIDDEN_SEGMENTS) {
            if (path.toLowerCase().contains(forbiddenSegment.toLowerCase())) {
                logger.error("Forbidden path segment detected: {} contains {}", path, forbiddenSegment);
                throw new SecurityValidationException("Forbidden path segment detected: " + forbiddenSegment, SecurityViolationType.FORBIDDEN_PATH_SEGMENT);
            }
        }
    }

    // === 散在していた templatePath.replace を置き換える専用メソッド ===

    /**
     * URL用のパス取得（ドット区切り）
     * 
     * 置き換え対象: templatePath.replace("/", ".")
     * 
     * @return ドット区切りのパス (例: "domain.point.molecules")
     */
    public String forUrl() {
        return originalPath;
    }

    /**
     * ファイルパス用のパス取得（スラッシュ区切り）
     * 
     * 置き換え対象: templatePath.replace(".", "/")
     * 
     * @return スラッシュ区切りのパス (例: "domain/point/molecules")
     */
    public String forFilePath() {
        return securePath;
    }

    /**
     * 表示用のパス取得（スラッシュ区切り、読みやすい形式）
     * 
     * @return 表示用パス (例: "domain/point/molecules")
     */
    public String forDisplay() {
        return securePath;
    }

    /**
     * Thymeleafテンプレート用のエンコード済みパス取得
     * 
     * 置き換え対象: model.addAttribute("templatePathEncoded", templatePath.replace("/", "."))
     * 
     * @return テンプレート用エンコード済みパス
     */
    public String forThymeleaf() {
        return originalPath;
    }

    /**
     * クラスパスリソース用のパス取得
     * 
     * 置き換え対象: "classpath:templates/" + templatePath.replace(".", "/") + ".html"
     * 
     * @param extension ファイル拡張子（例: ".html"）
     * @return クラスパスリソース用パス
     */
    public String forClasspath(String extension) {
        return "classpath:templates/" + securePath + Objects.toString(extension, "");
    }

    /**
     * パスの深さ（階層数）を取得
     */
    public int getDepth() {
        return (int) securePath.chars().filter(ch -> ch == '/').count() + 1;
    }

    /**
     * パスが指定したプレフィックスで始まるかどうかを判定
     */
    public boolean startsWith(String prefix) {
        return securePath.startsWith(prefix);
    }

    /**
     * パスの最初のセグメントを取得
     */
    public String getRootSegment() {
        int firstSlash = securePath.indexOf('/');
        return firstSlash == -1 ? securePath : securePath.substring(0, firstSlash);
    }

    /**
     * パスの最後のセグメントを取得
     */
    public String getLastSegment() {
        int lastSlash = securePath.lastIndexOf('/');
        return lastSlash == -1 ? securePath : securePath.substring(lastSlash + 1);
    }

    // === Object contract (Value Object として必須) ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecureTemplatePath that = (SecureTemplatePath) o;
        return Objects.equals(originalPath, that.originalPath) &&
               Objects.equals(securePath, that.securePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalPath, securePath);
    }

    @Override
    public String toString() {
        return "SecureTemplatePath{" +
                "original='" + originalPath + '\'' +
                ", secure='" + securePath + '\'' +
                '}';
    }

}