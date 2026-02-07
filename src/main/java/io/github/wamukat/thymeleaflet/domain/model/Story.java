package io.github.wamukat.thymeleaflet.domain.model;

import java.util.*;

/**
 * ストーリー - Domain Entity
 * 
 * Storybookストーリー（フラグメントの使用例・バリエーション）を表現するドメインエンティティ
 * Clean Architecture の原則に従い、ストーリーのビジネスルールとドメイン不変性を保持
 */
public class Story {

    private final StoryName name;
    private final Fragment parentFragment;
    private final Map<String, Object> parameters;
    private final Map<String, Object> metadata;
    private final StoryType storyType;

    /**
     * プライベートコンストラクタ - 不変Value Object設計
     * 
     * ファクトリメソッドのみからのインスタンス化を強制
     * Clean Architecture: 検証済み値による安全なオブジェクト生成
     */
    private Story(StoryName name, 
                Fragment parentFragment,
                Map<String, Object> parameters,
                Map<String, Object> metadata) {
        this.name = Objects.requireNonNull(name, "Story name cannot be null");
        this.parentFragment = Objects.requireNonNull(parentFragment, "Parent fragment cannot be null");
        
        // 防御的コピー + 不変化
        this.parameters = parameters != null ?
            Collections.unmodifiableMap(new HashMap<>(parameters)) :
            Collections.emptyMap();
        this.metadata = metadata != null ?
            Collections.unmodifiableMap(new HashMap<>(metadata)) :
            Collections.emptyMap();
            
        this.storyType = determineStoryType(name);
    }

    /**
     * プライベートコンストラクタ (基本版)
     */
    private Story(StoryName name, Fragment parentFragment) {
        this(name, parentFragment, 
             Collections.emptyMap(),    // parameters
             Collections.emptyMap());   // metadata
    }

    /**
     * Story作成 - ファクトリメソッド（完全指定版）
     */
    public static Story of(StoryName name, 
                          Fragment parentFragment,
                          Map<String, Object> parameters,
                          Map<String, Object> metadata) {
        return new Story(name, parentFragment, parameters, metadata);
    }

    /**
     * Story作成 - ファクトリメソッド（基本版）
     */
    public static Story of(StoryName name, Fragment parentFragment) {
        return new Story(name, parentFragment);
    }

    private StoryType determineStoryType(StoryName name) {
        if (name.isDefault()) return StoryType.DEFAULT;
        if (name.isPrimary()) return StoryType.PRIMARY;
        if (name.isSecondary()) return StoryType.SECONDARY;
        return StoryType.VARIANT;
    }

    // === ドメインメソッド ===

    /**
     * パラメータを設定した新しいStoryインスタンスを返す
     */
    public Story withParameter(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            return this; // 変更なし
        }
        Map<String, Object> newParams = new HashMap<>(this.parameters);
        newParams.put(key.trim(), value);
        return Story.of(name, parentFragment, newParams, metadata);
    }

    /**
     * 複数パラメータを設定した新しいStoryインスタンスを返す
     */
    public Story withParameters(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return this; // 変更なし
        }
        Map<String, Object> newParams = new HashMap<>(this.parameters);
        params.forEach((key, value) -> {
            if (key != null && !key.trim().isEmpty()) {
                newParams.put(key.trim(), value);
            }
        });
        return Story.of(name, parentFragment, newParams, metadata);
    }

    /**
     * メタデータを設定した新しいStoryインスタンスを返す
     */
    public Story withMetadata(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            return this; // 変更なし
        }
        Map<String, Object> newMetadata = new HashMap<>(this.metadata);
        newMetadata.put(key.trim(), value);
        return Story.of(name, parentFragment, parameters, newMetadata);
    }

    /**
     * ストーリーの説明を設定した新しいStoryインスタンスを返す
     */
    public Story withDescription(String description) {
        return withMetadata("description", description);
    }

    /**
     * ストーリーの実行可能性を検証
     */
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 必須パラメータの確認
        Set<String> requiredParams = parentFragment.getRequiredParameters();
        Set<String> missingParams = new HashSet<>(requiredParams);
        missingParams.removeAll(parameters.keySet());
        
        if (!missingParams.isEmpty()) {
            errors.add("Missing required parameters: " + missingParams);
        }

        // パラメータ値の検証
        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            if (param.getValue() == null) {
                warnings.add("Parameter '" + param.getKey() + "' has null value");
            }
        }

        // ストーリータイプ固有の検証
        validateStoryTypeSpecificRules(warnings);

        return ValidationResult.of(errors.isEmpty(), errors, warnings);
    }

    private void validateStoryTypeSpecificRules(List<String> warnings) {
        switch (storyType) {
            case DEFAULT:
                if (parameters.isEmpty() && !parentFragment.getRequiredParameters().isEmpty()) {
                    warnings.add("Default story should provide example values for required parameters");
                }
                break;
                
            case PRIMARY:
                if (parameters.isEmpty()) {
                    warnings.add("Primary story should demonstrate the main use case with parameters");
                }
                break;
                
            case SECONDARY:
                // セカンダリストーリーは代替的な使用例を示すべき
                break;
                
            case VARIANT:
                if (parameters.isEmpty()) {
                    warnings.add("Variant story should show different parameter combinations");
                }
                break;
        }
    }

    /**
     * ストーリーの使用例コードを生成
     */
    public String generateUsageExample() {
        return parentFragment.generateUsageExample(this.name);
    }

    // === 判定メソッド ===

    /**
     * 実行可能かを判定（必須パラメータがすべて設定されているか）
     */
    public boolean isExecutable() {
        ValidationResult validation = validate();
        return validation.isValid();
    }

    /**
     * デフォルトストーリーかを判定
     */
    public boolean isDefault() {
        return storyType == StoryType.DEFAULT;
    }

    // === 取得メソッド ===

    /**
     * ストーリーの説明を取得
     */
    public String getDescription() {
        String description = (String) metadata.get("description");
        return description != null ? description : "";
    }

    public StoryName getName() {
        return name;
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Story story = (Story) o;
        return Objects.equals(name, story.name) &&
               Objects.equals(parentFragment, story.parentFragment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parentFragment);
    }

    @Override
    public String toString() {
        return "Story{" +
                "name=" + name +
                ", type=" + storyType +
                ", parameters=" + parameters.size() +
                ", executable=" + isExecutable() +
                '}';
    }

    // === Enums ===

    public enum StoryType {
        DEFAULT("デフォルトの使用例"),
        PRIMARY("主要な使用例"),
        SECONDARY("代替的な使用例"),
        VARIANT("バリエーション例");

        private final String description;

        StoryType(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    // === 内部クラス ===

    /**
     * バリデーション結果 - Value Object (不変化版)
     * 
     * Clean Architecture: 不変Value Object設計
     * ファクトリメソッドパターン採用
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        /**
         * プライベートコンストラクタ - 不変Value Object設計
         * 
         * ファクトリメソッドのみからのインスタンス化を強制
         * Clean Architecture: 検証済み値による安全なオブジェクト生成
         */
        private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = Collections.unmodifiableList(
                errors != null ? new ArrayList<>(errors) : new ArrayList<>()
            );
            this.warnings = Collections.unmodifiableList(
                warnings != null ? new ArrayList<>(warnings) : new ArrayList<>()
            );
        }

        /**
         * ValidationResult作成 - ファクトリメソッド（完全指定版）
         */
        public static ValidationResult of(boolean valid, List<String> errors, List<String> warnings) {
            return new ValidationResult(valid, errors, warnings);
        }

        /**
         * 成功結果作成 - ファクトリメソッド（成功版）
         */
        public static ValidationResult success() {
            return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
        }

        /**
         * 成功結果作成 - ファクトリメソッド（警告付き成功版）
         */
        public static ValidationResult successWithWarnings(List<String> warnings) {
            return new ValidationResult(true, Collections.emptyList(), warnings);
        }

        /**
         * 失敗結果作成 - ファクトリメソッド（エラー版）
         */
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors, Collections.emptyList());
        }

        /**
         * 失敗結果作成 - ファクトリメソッド（エラー・警告付き版）
         */
        public static ValidationResult failure(List<String> errors, List<String> warnings) {
            return new ValidationResult(false, errors, warnings);
        }

        public boolean isValid() { return valid; }
        
        public List<String> getErrors() { return errors; }
        
        public List<String> getWarnings() { return warnings; }

        @Override
        public String toString() {
            return "ValidationResult{valid=" + valid + 
                   ", errors=" + errors.size() + 
                   ", warnings=" + warnings.size() + '}';
        }
    }
}
