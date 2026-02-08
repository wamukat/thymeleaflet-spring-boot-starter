package io.github.wamukat.thymeleaflet.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 多層型推論システムで使用する詳細な型情報を保持するクラス
 * <p>
 * Phase 1A: 基本的な型推論とJavaDoc型抽出機能
 * 既存のParameterInfoを拡張して、より詳細な型情報と推論結果を管理
 */
public class TypeInfo {

    /**
     * 型推論のレベル（確信度）
     */
    public enum InferenceLevel {
        /**
         * 明示的にJavaDocで定義されている（最高確信度）
         */
        EXPLICIT,
        /**
         * JavaDocの例から推論（高確信度）
         */
        INFERRED_FROM_EXAMPLE,
        /**
         * コンテキストから推論（中確信度）
         */
        INFERRED_FROM_CONTEXT,
        /**
         * デフォルトフォールバック（低確信度）
         */
        FALLBACK
    }

    /**
     * Java型の分類
     */
    public enum TypeCategory {
        /**
         * プリミティブ型 (String, Boolean, Integer等)
         */
        PRIMITIVE,
        /**
         * Enum型
         */
        ENUM,
        /**
         * コレクション型 (List, Set等)
         */
        COLLECTION,
        /**
         * カスタムオブジェクト型
         */
        OBJECT,
        /**
         * 不明な型
         */
        UNKNOWN
    }

    // 基本的な型情報
    private final String parameterName;
    private final String javaTypeName;
    private final TypeCategory typeCategory;
    private final boolean required;
    private final String description;

    // 型推論情報
    private final InferenceLevel inferenceLevel;
    private final String inferenceSource;  // 推論の根拠（例: "JavaDoc @param", "example argument"等）

    // 詳細型情報
    private final List<String> allowedValues;      // Enum値や制約値

    /**
     * プライベートコンストラクタ - 不変Value Object設計
     * 
     * ファクトリメソッドのみからのインスタンス化を強制
     * Clean Architecture: 検証済み値による安全なオブジェクト生成
     */
    private TypeInfo(String parameterName, String javaTypeName, TypeCategory typeCategory, 
                    boolean required, String description, InferenceLevel inferenceLevel, 
                    String inferenceSource, List<String> allowedValues) {
        this.parameterName = Objects.requireNonNull(parameterName, "parameterName cannot be null");
        this.javaTypeName = Objects.requireNonNull(javaTypeName, "javaTypeName cannot be null");
        this.typeCategory = Objects.requireNonNull(typeCategory, "typeCategory cannot be null");
        this.required = required;
        this.description = Objects.requireNonNullElse(description, "");
        this.inferenceLevel = Objects.requireNonNull(inferenceLevel, "inferenceLevel cannot be null");
        this.inferenceSource = Objects.requireNonNullElse(inferenceSource, "");
        this.allowedValues = Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNullElse(allowedValues, List.<String>of())));
    }

    /**
     * TypeInfo作成 - ファクトリメソッド（完全指定版）
     */
    public static TypeInfo of(String parameterName, String javaTypeName, TypeCategory typeCategory, 
                             boolean required, String description, InferenceLevel inferenceLevel, 
                             String inferenceSource, List<String> allowedValues) {
        return new TypeInfo(parameterName, javaTypeName, typeCategory, required, description, 
                           inferenceLevel, inferenceSource, allowedValues);
    }

    /**
     * TypeInfo作成 - ファクトリメソッド（基本版）
     */
    public static TypeInfo of(String parameterName, String javaTypeName, TypeCategory typeCategory) {
        return new TypeInfo(parameterName, javaTypeName, typeCategory, false, "",
                           InferenceLevel.FALLBACK, "", Collections.emptyList());
    }

    /**
     * TypeInfo作成 - ファクトリメソッド（推論情報付き）
     */
    public static TypeInfo of(String parameterName, String javaTypeName, TypeCategory typeCategory, 
                             InferenceLevel inferenceLevel, String inferenceSource) {
        return new TypeInfo(parameterName, javaTypeName, typeCategory, false, "",
                           inferenceLevel, inferenceSource, Collections.emptyList());
    }

    /**
     * プリミティブ型TypeInfo作成 - 事前定義ファクトリメソッド
     */
    public static TypeInfo primitiveType(String parameterName, String javaTypeName) {
        return new TypeInfo(parameterName, javaTypeName, TypeCategory.PRIMITIVE, false, "",
                           InferenceLevel.FALLBACK, "", Collections.emptyList());
    }

    /**
     * Enum型TypeInfo作成 - 事前定義ファクトリメソッド
     */
    public static TypeInfo enumType(String parameterName, String javaTypeName, List<String> allowedValues) {
        return new TypeInfo(parameterName, javaTypeName, TypeCategory.ENUM, false, "",
                           InferenceLevel.EXPLICIT, "Enum definition", allowedValues);
    }

    /**
     * 不明型TypeInfo作成 - 事前定義ファクトリメソッド
     */
    public static TypeInfo unknownType(String parameterName) {
        return new TypeInfo(parameterName, "Object", TypeCategory.UNKNOWN, false, "",
                           InferenceLevel.FALLBACK, "Unknown type fallback", Collections.emptyList());
    }

    // Getters
    public String getParameterName() {
        return parameterName;
    }

    public String getJavaTypeName() {
        return javaTypeName;
    }

    public TypeCategory getTypeCategory() {
        return typeCategory;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public InferenceLevel getInferenceLevel() {
        return inferenceLevel;
    }

    public String getInferenceSource() {
        return inferenceSource;
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }

    /**
     * Enum型かどうかを判定
     */
    public boolean isEnum() {
        return typeCategory == TypeCategory.ENUM;
    }

    @Override
    public String toString() {
        return String.format("TypeInfo{name='%s', type='%s', category=%s, level=%s, source='%s'}",
                parameterName, javaTypeName, typeCategory, inferenceLevel, inferenceSource);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TypeInfo typeInfo = (TypeInfo) obj;
        return Objects.equals(parameterName, typeInfo.parameterName) &&
               Objects.equals(javaTypeName, typeInfo.javaTypeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterName, javaTypeName);
    }

    /**
     * TypeInfo構築用のBuilderクラス（後方互換性維持）
     */
    public static class Builder {
        private String parameterName = "";
        private String javaTypeName = "";
        private TypeCategory typeCategory = TypeCategory.UNKNOWN;
        private boolean required = false;
        private String description = "";
        private InferenceLevel inferenceLevel = InferenceLevel.FALLBACK;
        private String inferenceSource = "";
        private List<String> allowedValues = new ArrayList<>();

        public Builder parameterName(String parameterName) {
            this.parameterName = Objects.requireNonNull(parameterName, "parameterName cannot be null");
            return this;
        }

        public Builder javaTypeName(String javaTypeName) {
            this.javaTypeName = Objects.requireNonNull(javaTypeName, "javaTypeName cannot be null");
            return this;
        }

        public Builder typeCategory(TypeCategory typeCategory) {
            this.typeCategory = Objects.requireNonNull(typeCategory, "typeCategory cannot be null");
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder description(String description) {
            this.description = Objects.requireNonNullElse(description, "");
            return this;
        }

        public Builder inferenceLevel(InferenceLevel inferenceLevel) {
            this.inferenceLevel = Objects.requireNonNull(inferenceLevel, "inferenceLevel cannot be null");
            return this;
        }

        public Builder allowedValues(List<String> allowedValues) {
            this.allowedValues = Objects.requireNonNullElse(allowedValues, new ArrayList<>());
            return this;
        }

        public TypeInfo build() {
            // 必須フィールドの検証
            if (parameterName.trim().isEmpty()) {
                throw new IllegalArgumentException("parameterName is required");
            }
            if (javaTypeName.trim().isEmpty()) {
                throw new IllegalArgumentException("javaTypeName is required");
            }

            return new TypeInfo(parameterName, javaTypeName, typeCategory, required, description, 
                               inferenceLevel, inferenceSource, allowedValues);
        }
    }
}
