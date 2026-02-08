package io.github.wamukat.thymeleaflet.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * フラグメント名 - Domain Value Object
 * 
 * Thymeleafフラグメントの名前を表現する不変オブジェクト
 * Clean Architectureの原則に従い、ドメインルールとバリデーションを内包
 */
public final class FragmentName {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_-]*$");
    private static final int MAX_LENGTH = 100;
    private static final int MIN_LENGTH = 1;

    private final String value;

    /**
     * プライベートコンストラクタ - 不変Value Object設計
     * 
     * ファクトリメソッドのみからのインスタンス化を強制
     * Clean Architecture: 検証済み値による安全なオブジェクト生成
     */
    private FragmentName(String validatedValue) {
        this.value = Objects.requireNonNull(validatedValue, "Validated value cannot be null");
    }

    /**
     * フラグメント名を作成 - ファクトリメソッド
     * 
     * @param value フラグメント名の文字列
     * @return 検証済みのFragmentNameインスタンス
     * @throws IllegalArgumentException 無効なフラグメント名の場合
     */
    public static FragmentName of(String value) {
        String validatedValue = validateAndNormalize(value);
        return new FragmentName(validatedValue);
    }

    /**
     * 標準フラグメント名を作成 - 事前定義ファクトリメソッド
     */
    public static FragmentName fragment() {
        return new FragmentName("fragment");
    }

    /**
     * デフォルトフラグメント名を作成 - 事前定義ファクトリメソッド
     */
    public static FragmentName defaultFragment() {
        return new FragmentName("default");
    }

    /**
     * メインフラグメント名を作成 - 事前定義ファクトリメソッド
     */
    public static FragmentName main() {
        return new FragmentName("main");
    }

    private static String validateAndNormalize(String value) {
        String trimmed = Objects.requireNonNull(value, "Fragment name cannot be null").trim();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Fragment name cannot be empty");
        }

        if (trimmed.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Fragment name must be at least " + MIN_LENGTH + " characters long");
        }

        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Fragment name cannot exceed " + MAX_LENGTH + " characters");
        }

        if (!VALID_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Fragment name must start with alphanumeric character and contain only alphanumeric, underscore, or hyphen characters: " + trimmed
            );
        }

        return trimmed;
    }

    /**
     * PascalCaseかどうかを判定
     */
    public boolean isPascalCase() {
        return value.matches("^[A-Z][a-zA-Z0-9]*$");
    }

    /**
     * kebab-caseかどうかを判定
     */
    public boolean isKebabCase() {
        return value.matches("^[a-z][a-z0-9-]*$");
    }

    /**
     * snake_caseかどうかを判定
     */
    public boolean isSnakeCase() {
        return value.matches("^[a-z][a-z0-9_]*$");
    }

    /**
     * フラグメント名の種別を取得
     */
    public FragmentNameType getType() {
        if (isPascalCase()) {
            return FragmentNameType.PASCAL_CASE;
        } else if (isKebabCase()) {
            return FragmentNameType.KEBAB_CASE;
        } else if (isSnakeCase()) {
            return FragmentNameType.SNAKE_CASE;
        } else {
            return FragmentNameType.MIXED;
        }
    }

    public String getValue() {
        return value;
    }

    public int getLength() {
        return value.length();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FragmentName that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public enum FragmentNameType {
        PASCAL_CASE(),
        KEBAB_CASE(),
        SNAKE_CASE(),
        MIXED();

        FragmentNameType() {
        }

    }
}
