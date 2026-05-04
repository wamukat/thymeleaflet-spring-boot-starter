package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * HTMLテンプレート内のJavaDocコメント解析Infrastructure実装
 */
@Component
public class JavaDocAnalyzer {

    private final JavaDocCommentBlockExtractor commentBlockExtractor;
    private final JavaDocTagParser tagParser;
    private final JavaDocExampleParser exampleParser;

    public JavaDocAnalyzer() {
        this(new StructuredTemplateParser());
    }

    JavaDocAnalyzer(StructuredTemplateParser templateParser) {
        this(new JavaDocCommentBlockExtractor(), new JavaDocTagParser(), new JavaDocExampleParser(templateParser));
    }

    JavaDocAnalyzer(
        JavaDocCommentBlockExtractor commentBlockExtractor,
        JavaDocTagParser tagParser,
        JavaDocExampleParser exampleParser
    ) {
        this.commentBlockExtractor = Objects.requireNonNull(commentBlockExtractor, "commentBlockExtractor cannot be null");
        this.tagParser = Objects.requireNonNull(tagParser, "tagParser cannot be null");
        this.exampleParser = Objects.requireNonNull(exampleParser, "exampleParser cannot be null");
    }

    /**
     * HTMLテンプレートからJavaDocコメントを解析
     */
    public List<JavaDocInfo> analyzeJavaDocFromHtml(String htmlContent) {
        return analyzeJavaDocFromHtml(htmlContent, "");
    }

    public List<JavaDocInfo> analyzeJavaDocFromHtml(String htmlContent, String currentTemplatePath) {
        Objects.requireNonNull(htmlContent, "htmlContent cannot be null");
        List<JavaDocInfo> docInfoList = new ArrayList<>();

        if (htmlContent.trim().isEmpty()) {
            return docInfoList;
        }

        for (String javadocContent : commentBlockExtractor.extract(htmlContent)) {
            JavaDocTagParser.ParsedTags parsedTags = tagParser.parse(javadocContent);
            docInfoList.add(JavaDocInfo.of(
                parsedTags.description(),
                parsedTags.parameters(),
                parsedTags.models(),
                parsedTags.fragmentName(),
                exampleParser.parse(javadocContent, currentTemplatePath),
                parsedTags.backgroundColor()
            ));
        }

        return docInfoList;
    }
    
    /**
     * JavaDoc情報を保持するクラス - Value Object (不変化版)
     * 
     * Clean Architecture: 不変Value Object設計
     * TypeInfo.javaと同様のファクトリメソッドパターン採用
     */
    public static class JavaDocInfo {
        private final String description;
        private final List<ParameterInfo> parameters;
        private final List<ModelInfo> models;
        private final Optional<String> fragmentName;
        private final List<ExampleInfo> examples;
        private final Optional<String> backgroundColor;
        
        /**
         * プライベートコンストラクタ - 不変Value Object設計
         * 
         * ファクトリメソッドのみからのインスタンス化を強制
         * Clean Architecture: 検証済み値による安全なオブジェクト生成
         */
        private JavaDocInfo(
            String description,
            List<ParameterInfo> parameters,
            List<ModelInfo> models,
            Optional<String> fragmentName,
            List<ExampleInfo> examples,
            Optional<String> backgroundColor
        ) {
            this.description = description;
            this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
            this.models = Collections.unmodifiableList(new ArrayList<>(models));
            this.fragmentName = Objects.requireNonNull(fragmentName, "fragmentName cannot be null");
            this.examples = Collections.unmodifiableList(new ArrayList<>(examples));
            this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor cannot be null");
        }
        
        /**
         * JavaDocInfo作成 - ファクトリメソッド（完全指定版）
         */
        public static JavaDocInfo of(
            String description,
            List<ParameterInfo> parameters,
            List<ModelInfo> models,
            List<ExampleInfo> examples,
            Optional<String> backgroundColor
        ) {
            return new JavaDocInfo(description, parameters, models, Optional.empty(), examples, backgroundColor);
        }

        public static JavaDocInfo of(
            String description,
            List<ParameterInfo> parameters,
            List<ModelInfo> models,
            Optional<String> fragmentName,
            List<ExampleInfo> examples,
            Optional<String> backgroundColor
        ) {
            return new JavaDocInfo(description, parameters, models, fragmentName, examples, backgroundColor);
        }

        public static JavaDocInfo of(
            String description,
            List<ParameterInfo> parameters,
            List<ModelInfo> models,
            List<ExampleInfo> examples,
            String backgroundColor
        ) {
            return new JavaDocInfo(description, parameters, models, Optional.empty(), examples, Optional.ofNullable(backgroundColor));
        }

        /**
         * JavaDocInfo作成 - ファクトリメソッド（モデル省略版）
         */
        public static JavaDocInfo of(
            String description,
            List<ParameterInfo> parameters,
            List<ExampleInfo> examples,
            Optional<String> backgroundColor
        ) {
            return new JavaDocInfo(description, parameters, Collections.emptyList(), Optional.empty(), examples, backgroundColor);
        }

        public static JavaDocInfo of(
            String description,
            List<ParameterInfo> parameters,
            List<ExampleInfo> examples,
            String backgroundColor
        ) {
            return new JavaDocInfo(description, parameters, Collections.emptyList(), Optional.empty(), examples, Optional.ofNullable(backgroundColor));
        }
        
        /**
         * JavaDocInfo作成 - ファクトリメソッド（基本版）
         */
        public static JavaDocInfo of(String description) {
            return new JavaDocInfo(description, Collections.emptyList(), Collections.emptyList(), Optional.empty(), Collections.emptyList(), Optional.empty());
        }
        
        // Getters
        public String getDescription() { return description; }
        public List<ParameterInfo> getParameters() { return parameters; }
        public List<ModelInfo> getModels() { return models; }
        public String getFragmentName() { return fragmentName.orElse(""); }
        public Optional<String> getFragmentNameOptional() { return fragmentName; }
        public List<ExampleInfo> getExamples() { return examples; }
        public String getBackgroundColor() { return backgroundColor.orElse(""); }
        public Optional<String> getBackgroundColorOptional() { return backgroundColor; }
        
        @Override
        public String toString() {
            return String.format("JavaDocInfo{description='%s', parameters=%d, models=%d, fragment='%s', examples=%d, background='%s'}",
                               description, parameters.size(), models.size(), fragmentName.orElse(""), examples.size(), backgroundColor.orElse(""));
        }
    }

    /**
     * モデル情報を保持するクラス - Value Object (不変化版)
     *
     * Clean Architecture: 不変Value Object設計
     * TypeInfo.javaと同様のファクトリメソッドパターン採用
     */
    public static class ModelInfo {
        private final String name;
        private final String type;
        private final boolean required;
        private final Optional<String> defaultValue;
        private final Optional<String> description;

        /**
         * プライベートコンストラクタ - 不変Value Object設計
         *
         * ファクトリメソッドのみからのインスタンス化を強制
         * Clean Architecture: 検証済み値による安全なオブジェクト生成
         */
        private ModelInfo(String name, String type, boolean required, Optional<String> defaultValue, Optional<String> description) {
            this.name = java.util.Objects.requireNonNull(name, "name cannot be null");
            this.type = java.util.Objects.requireNonNull(type, "type cannot be null");
            this.required = required;
            this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue cannot be null");
            this.description = Objects.requireNonNull(description, "description cannot be null");
        }

        /**
         * ModelInfo作成 - ファクトリメソッド（完全指定版）
         */
        public static ModelInfo of(String name, String type, boolean required, Optional<String> defaultValue, Optional<String> description) {
            return new ModelInfo(name, type, required, defaultValue, description);
        }

        public static ModelInfo of(String name, String type, boolean required, String defaultValue, String description) {
            return new ModelInfo(name, type, required, Optional.ofNullable(defaultValue), Optional.ofNullable(description));
        }

        /**
         * ModelInfo作成 - ファクトリメソッド（基本版）
         */
        public static ModelInfo of(String name, String type) {
            return new ModelInfo(name, type, false, Optional.empty(), Optional.empty());
        }

        /**
         * ModelInfo作成 - ファクトリメソッド（必須属性版）
         */
        public static ModelInfo required(String name, String type, Optional<String> description) {
            return new ModelInfo(name, type, true, Optional.empty(), description);
        }

        public static ModelInfo required(String name, String type, String description) {
            return new ModelInfo(name, type, true, Optional.empty(), Optional.ofNullable(description));
        }

        /**
         * ModelInfo作成 - ファクトリメソッド（オプション属性版）
         */
        public static ModelInfo optional(String name, String type, Optional<String> defaultValue, Optional<String> description) {
            return new ModelInfo(name, type, false, defaultValue, description);
        }

        public static ModelInfo optional(String name, String type, String defaultValue, String description) {
            return new ModelInfo(name, type, false, Optional.ofNullable(defaultValue), Optional.ofNullable(description));
        }

        // Getters
        public String getName() { return name; }
        public String getType() { return type; }
        public boolean isRequired() { return required; }
        public String getDefaultValue() { return defaultValue.orElse(""); }
        public Optional<String> getDefaultValueOptional() { return defaultValue; }
        public String getDescription() { return description.orElse(""); }
        public Optional<String> getDescriptionOptional() { return description; }

        @Override
        public String toString() {
            return String.format("ModelInfo{name='%s', type='%s', required=%s, default='%s'}", 
                               name, type, required, defaultValue);
        }
    }
    
    /**
     * パラメータ情報を保持するクラス - Value Object (不変化版)
     * 
     * Clean Architecture: 不変Value Object設計
     * TypeInfo.javaと同様のファクトリメソッドパターン採用
     */
    public static class ParameterInfo {
        private final String name;
        private final String type;
        private final boolean required;
        private final Optional<String> defaultValue;
        private final Optional<String> description;
        private final List<String> allowedValues;
        
        /**
         * プライベートコンストラクタ - 不変Value Object設計
         * 
         * ファクトリメソッドのみからのインスタンス化を強制
         * Clean Architecture: 検証済み値による安全なオブジェクト生成
         */
        private ParameterInfo(
            String name,
            String type,
            boolean required,
            Optional<String> defaultValue,
            Optional<String> description,
            List<String> allowedValues
        ) {
            this.name = java.util.Objects.requireNonNull(name, "name cannot be null");
            this.type = java.util.Objects.requireNonNull(type, "type cannot be null");
            this.required = required;
            this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue cannot be null");
            this.description = Objects.requireNonNull(description, "description cannot be null");
            this.allowedValues = Collections.unmodifiableList(new ArrayList<>(allowedValues));
        }
        
        /**
         * ParameterInfo作成 - ファクトリメソッド（完全指定版）
         */
        public static ParameterInfo of(
            String name,
            String type,
            boolean required,
            Optional<String> defaultValue,
            Optional<String> description,
            List<String> allowedValues
        ) {
            return new ParameterInfo(name, type, required, defaultValue, description, allowedValues);
        }

        public static ParameterInfo of(
            String name,
            String type,
            boolean required,
            String defaultValue,
            String description,
            List<String> allowedValues
        ) {
            return new ParameterInfo(name, type, required, Optional.ofNullable(defaultValue), Optional.ofNullable(description), allowedValues);
        }
        
        /**
         * ParameterInfo作成 - ファクトリメソッド（基本版）
         */
        public static ParameterInfo of(String name, String type) {
            return new ParameterInfo(name, type, false, Optional.empty(), Optional.empty(), Collections.emptyList());
        }
        
        /**
         * ParameterInfo作成 - ファクトリメソッド（必須属性版）
         */
        public static ParameterInfo required(String name, String type, Optional<String> description) {
            return new ParameterInfo(name, type, true, Optional.empty(), description, Collections.emptyList());
        }

        public static ParameterInfo required(String name, String type, String description) {
            return new ParameterInfo(name, type, true, Optional.empty(), Optional.ofNullable(description), Collections.emptyList());
        }
        
        /**
         * ParameterInfo作成 - ファクトリメソッド（オプション属性版）
         */
        public static ParameterInfo optional(String name, String type, Optional<String> defaultValue, Optional<String> description) {
            return new ParameterInfo(name, type, false, defaultValue, description, Collections.emptyList());
        }

        public static ParameterInfo optional(String name, String type, String defaultValue, String description) {
            return new ParameterInfo(name, type, false, Optional.ofNullable(defaultValue), Optional.ofNullable(description), Collections.emptyList());
        }
        
        // Getters
        public String getName() { return name; }
        public String getType() { return type; }
        public boolean isRequired() { return required; }
        public String getDefaultValue() { return defaultValue.orElse(""); }
        public Optional<String> getDefaultValueOptional() { return defaultValue; }
        public String getDescription() { return description.orElse(""); }
        public Optional<String> getDescriptionOptional() { return description; }
        public List<String> getAllowedValues() { return allowedValues; }
        
        @Override
        public String toString() {
            return String.format("ParameterInfo{name='%s', type='%s', required=%s, default='%s'}", 
                               name, type, required, defaultValue);
        }
    }
    
    /**
     * 使用例情報を保持するクラス - Value Object (不変化版)
     * 
     * Clean Architecture: 不変Value Object設計
     * TypeInfo.javaと同様のファクトリメソッドパターン採用
     */
    public static class ExampleInfo {
        private final String templatePath;
        private final String fragmentName;
        private final List<String> arguments;
        
        /**
         * プライベートコンストラクタ - 不変Value Object設計
         * 
         * ファクトリメソッドのみからのインスタンス化を強制
         * Clean Architecture: 検証済み値による安全なオブジェクト生成
         */
        private ExampleInfo(String templatePath, String fragmentName, List<String> arguments) {
            this.templatePath = java.util.Objects.requireNonNull(templatePath, "templatePath cannot be null");
            this.fragmentName = java.util.Objects.requireNonNull(fragmentName, "fragmentName cannot be null");
            this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
        }
        
        /**
         * ExampleInfo作成 - ファクトリメソッド（完全指定版）
         */
        public static ExampleInfo of(String templatePath, String fragmentName, List<String> arguments) {
            return new ExampleInfo(templatePath, fragmentName, arguments);
        }
        
        /**
         * ExampleInfo作成 - ファクトリメソッド（基本版）
         */
        public static ExampleInfo of(String templatePath, String fragmentName) {
            return new ExampleInfo(templatePath, fragmentName, Collections.emptyList());
        }
        
        // Getters
        public String getTemplatePath() { return templatePath; }
        public String getFragmentName() { return fragmentName; }
        public List<String> getArguments() { return arguments; }
        
        @Override
        public String toString() {
            return String.format("ExampleInfo{template='%s', fragment='%s', args=%s}", 
                               templatePath, fragmentName, arguments);
        }
    }
}
