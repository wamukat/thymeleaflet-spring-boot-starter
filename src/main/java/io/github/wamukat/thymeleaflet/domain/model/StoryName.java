package io.github.wamukat.thymeleaflet.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * ストーリー名 - Domain Value Object
 * 
 * Storybookストーリーの名前を表現する不変オブジェクト
 * Clean Architectureの原則に従い、ストーリー命名のドメインルールを内包
 */
public final class StoryName {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_-]*$");
    private static final int MAX_LENGTH = 100;
    private static final int MIN_LENGTH = 1;
    
    // 定義済みストーリー名（Storybook標準）
    private static final String DEFAULT_STORY = "default";
    private static final String PRIMARY_STORY = "primary";
    private static final String SECONDARY_STORY = "secondary";

    private final String value;

    /**
     * プライベートコンストラクタ - 不変Value Object設計
     * 
     * ファクトリメソッドのみからのインスタンス化を強制
     * Clean Architecture: 検証済み値による安全なオブジェクト生成
     */
    private StoryName(String validatedValue) {
        this.value = Objects.requireNonNull(validatedValue, "Validated value cannot be null");
    }

    /**
     * ストーリー名を作成 - ファクトリメソッド
     * 
     * @param value ストーリー名の文字列
     * @return 検証済みのStoryNameインスタンス
     * @throws IllegalArgumentException 無効なストーリー名の場合
     */
    public static StoryName of(String value) {
        String validatedValue = validateAndNormalize(value);
        return new StoryName(validatedValue);
    }

    /**
     * デフォルトストーリー名を作成 - 事前定義ファクトリメソッド
     */
    public static StoryName defaultStory() {
        return new StoryName(DEFAULT_STORY);
    }

    /**
     * プライマリストーリー名を作成 - 事前定義ファクトリメソッド
     */
    public static StoryName primaryStory() {
        return new StoryName(PRIMARY_STORY);
    }

    /**
     * セカンダリストーリー名を作成 - 事前定義ファクトリメソッド
     */
    public static StoryName secondaryStory() {
        return new StoryName(SECONDARY_STORY);
    }

    private static String validateAndNormalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Story name cannot be null");
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Story name cannot be empty");
        }

        if (trimmed.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Story name must be at least " + MIN_LENGTH + " characters long");
        }

        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Story name cannot exceed " + MAX_LENGTH + " characters");
        }

        if (!VALID_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Story name must start with alphanumeric character and contain only alphanumeric, underscore, or hyphen characters: " + trimmed
            );
        }

        return trimmed;
    }

    /**
     * デフォルトストーリーかどうかを判定
     */
    public boolean isDefault() {
        return DEFAULT_STORY.equals(value);
    }

    /**
     * プライマリストーリーかどうかを判定
     */
    public boolean isPrimary() {
        return PRIMARY_STORY.equals(value);
    }

    /**
     * セカンダリストーリーかどうかを判定
     */
    public boolean isSecondary() {
        return SECONDARY_STORY.equals(value);
    }

    /**
     * ストーリーの種別を取得
     */
    public StoryType getType() {
        if (isDefault()) return StoryType.DEFAULT;
        if (isPrimary()) return StoryType.PRIMARY;
        if (isSecondary()) return StoryType.SECONDARY;
        return StoryType.CUSTOM;
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
        if (o == null || getClass() != o.getClass()) return false;
        StoryName storyName = (StoryName) o;
        return Objects.equals(value, storyName.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public enum StoryType {
        DEFAULT(),
        PRIMARY(),
        SECONDARY(),
        CUSTOM();

        StoryType() {
        }

    }
}