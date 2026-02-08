package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTMLテンプレート内のJavaDocコメント解析Infrastructure実装
 * Legacy実装の完全移設（既存の解析結果との互換性維持を優先）
 */
@Component
public class JavaDocAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(JavaDocAnalyzer.class);
    
    // Legacy実装からの完全コピー
    private static final Pattern JAVADOC_PATTERN = Pattern.compile(
        "<!--[^>]*?/\\*\\*([\\s\\S]*?)\\*/[^<]*?-->", 
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern PARAM_PATTERN = Pattern.compile(
        "@param\\s+(\\w+)\\s+\\{@code\\s+([^}]+?)\\}\\s+\\[(required|optional(?:=[^\\]]*)?)\\]\\s+([\\s\\S]*?)(?=\\s*@param|\\s*@model|\\s*@example|\\s*@background|\\s*\\*/|$)",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    private static final Pattern MODEL_PATTERN = Pattern.compile(
        "@model\\s+(\\w+)\\s+\\{@code\\s+([^}]+?)\\}\\s+\\[(required|optional(?:=[^\\]]*)?)\\]\\s+([\\s\\S]*?)(?=\\s*@param|\\s*@model|\\s*@example|\\s*@background|\\s*\\*/|$)",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern EXAMPLE_PATTERN = Pattern.compile(
        "@example\\s+.*?<(div|span)[^>]*th:replace=\"~\\{([^}]+?)\\s*::\\s*([^\\(\\}]+?)\\(([^)]*)\\)\\}\"[^>]*></(div|span)>",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern BACKGROUND_PATTERN = Pattern.compile(
        "@background\\s+(\\S+)",
        Pattern.MULTILINE
    );

    /**
     * HTMLテンプレートからJavaDocコメントを解析 - Legacy実装移設
     */
    public List<JavaDocInfo> analyzeJavaDocFromHtml(String htmlContent) {
        Objects.requireNonNull(htmlContent, "htmlContent cannot be null");
        List<JavaDocInfo> docInfoList = new ArrayList<>();

        if (htmlContent.trim().isEmpty()) {
            return docInfoList;
        }
        
        Matcher javadocMatcher = JAVADOC_PATTERN.matcher(htmlContent);
        
        while (javadocMatcher.find()) {
            String javadocContent = javadocMatcher.group(1);
            
            // 技術的データ抽出処理
            List<ParameterInfo> parameters = parseParameters(javadocContent);
            List<ModelInfo> models = parseModels(javadocContent);
            List<ExampleInfo> examples = parseExamples(javadocContent);
            Optional<String> backgroundColor = parseBackgroundColor(javadocContent);
            String description = extractDescription(javadocContent);
            
            JavaDocInfo docInfo = JavaDocInfo.of(description, parameters, models, examples, backgroundColor);
            docInfoList.add(docInfo);
        }
        
        return docInfoList;
    }
    
    /**
     * @paramタグからパラメータ情報を解析
     */
    private List<ParameterInfo> parseParameters(String javadocContent) {
        List<ParameterInfo> parameters = new ArrayList<>();
        
        Matcher paramMatcher = PARAM_PATTERN.matcher(javadocContent);
        while (paramMatcher.find()) {
            String name = paramMatcher.group(1);
            String type = paramMatcher.group(2);
            String requiredOrOptional = paramMatcher.group(3);
            String fullDescription = paramMatcher.group(4);
            
            // デフォルト値の抽出
            Optional<String> defaultValue = Optional.empty();
            if (requiredOrOptional.startsWith("optional=")) {
                String rawDefaultValue = requiredOrOptional.substring("optional=".length());
                if (!"null".equals(rawDefaultValue)) {
                    defaultValue = Optional.of(rawDefaultValue);
                }
            }
            
            boolean required = "required".equals(requiredOrOptional);
            
            // 説明文から許可値を分離
            String description = fullDescription;
            List<String> allowedValues = Collections.emptyList();
            
            // values: "value1", "value2" パターンを検索
            Pattern valuesPattern = Pattern.compile("。\\s*values:\\s*(.+?)(?=\\s*$)", Pattern.MULTILINE);
            Matcher valuesMatcher = valuesPattern.matcher(fullDescription);
            if (valuesMatcher.find()) {
                String valuesStr = valuesMatcher.group(1);
                allowedValues = parseAllowedValues(valuesStr);
                description = fullDescription.replaceAll("。\\s*values:\\s*.+$", "").trim();
            }
            
            description = description.replaceAll("\\s+", " ").trim();
            
            parameters.add(ParameterInfo.of(name, type, required, defaultValue, Optional.of(description), allowedValues));
        }
        
        return parameters;
    }

    /**
     * @modelタグからモデル情報を解析
     */
    private List<ModelInfo> parseModels(String javadocContent) {
        List<ModelInfo> models = new ArrayList<>();

        Matcher modelMatcher = MODEL_PATTERN.matcher(javadocContent);
        while (modelMatcher.find()) {
            String name = modelMatcher.group(1);
            String type = modelMatcher.group(2);
            String requiredOrOptional = modelMatcher.group(3);
            String fullDescription = modelMatcher.group(4);

            Optional<String> defaultValue = Optional.empty();
            if (requiredOrOptional.startsWith("optional=")) {
                String rawDefaultValue = requiredOrOptional.substring("optional=".length());
                if (!"null".equals(rawDefaultValue)) {
                    defaultValue = Optional.of(rawDefaultValue);
                }
            }

            boolean required = "required".equals(requiredOrOptional);
            String description = fullDescription.replaceAll("\\s+", " ").trim();

            models.add(ModelInfo.of(name, type, required, defaultValue, Optional.of(description)));
        }

        return models;
    }
    
    private List<String> parseAllowedValues(String valuesStr) {
        if (valuesStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> values = new ArrayList<>();
        Pattern valuePattern = Pattern.compile("\"([^\"]+)\"");
        Matcher valueMatcher = valuePattern.matcher(valuesStr);
        while (valueMatcher.find()) {
            values.add(valueMatcher.group(1));
        }
        
        return values;
    }
    
    /**
     * @exampleタグから使用例を解析
     */
    private List<ExampleInfo> parseExamples(String javadocContent) {
        List<ExampleInfo> examples = new ArrayList<>();
        
        Matcher exampleMatcher = EXAMPLE_PATTERN.matcher(javadocContent);
        while (exampleMatcher.find()) {
            String templatePath = exampleMatcher.group(2).trim();
            String fragmentName = exampleMatcher.group(3).trim();
            String argumentsStr = exampleMatcher.group(4);
            
            List<String> arguments = parseArguments(argumentsStr);
            examples.add(ExampleInfo.of(templatePath, fragmentName, arguments));
        }
        return examples;
    }
    
    /**
     * @backgroundタグから背景色を解析
     */
    private Optional<String> parseBackgroundColor(String javadocContent) {
        Matcher backgroundMatcher = BACKGROUND_PATTERN.matcher(javadocContent);
        if (backgroundMatcher.find()) {
            return Optional.of(backgroundMatcher.group(1));
        }
        return Optional.empty();
    }
    
    /**
     * 引数文字列をパース
     */
    private List<String> parseArguments(String argumentsStr) {
        if (argumentsStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> arguments = new ArrayList<>();
        String[] parts = argumentsStr.split(",(?=(?:[^']*'[^']*')*[^']*$)");
        for (String part : parts) {
            arguments.add(part.trim());
        }
        
        return arguments;
    }
    
    /**
     * 説明文を抽出
     */
    private String extractDescription(String javadocContent) {
        String[] lines = javadocContent.split("\n");
        StringBuilder description = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("*")) {
                line = line.substring(1).trim();
            }
            
            if (line.startsWith("@")) {
                break;
            }
            
            if (!line.isEmpty()) {
                if (description.length() > 0) {
                    description.append("\n");
                }
                description.append(line);
            }
        }
        
        return description.toString();
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
            List<ExampleInfo> examples,
            Optional<String> backgroundColor
        ) {
            this.description = description;
            this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
            this.models = Collections.unmodifiableList(new ArrayList<>(models));
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
            return new JavaDocInfo(description, parameters, models, examples, backgroundColor);
        }

        public static JavaDocInfo of(
            String description,
            List<ParameterInfo> parameters,
            List<ModelInfo> models,
            List<ExampleInfo> examples,
            String backgroundColor
        ) {
            return new JavaDocInfo(description, parameters, models, examples, Optional.ofNullable(backgroundColor));
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
            return new JavaDocInfo(description, parameters, Collections.emptyList(), examples, backgroundColor);
        }

        public static JavaDocInfo of(
            String description,
            List<ParameterInfo> parameters,
            List<ExampleInfo> examples,
            String backgroundColor
        ) {
            return new JavaDocInfo(description, parameters, Collections.emptyList(), examples, Optional.ofNullable(backgroundColor));
        }
        
        /**
         * JavaDocInfo作成 - ファクトリメソッド（基本版）
         */
        public static JavaDocInfo of(String description) {
            return new JavaDocInfo(description, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Optional.empty());
        }
        
        // Getters
        public String getDescription() { return description; }
        public List<ParameterInfo> getParameters() { return parameters; }
        public List<ModelInfo> getModels() { return models; }
        public List<ExampleInfo> getExamples() { return examples; }
        public String getBackgroundColor() { return backgroundColor.orElse(""); }
        public Optional<String> getBackgroundColorOptional() { return backgroundColor; }
        
        @Override
        public String toString() {
            return String.format("JavaDocInfo{description='%s', parameters=%d, models=%d, examples=%d, background='%s'}",
                               description, parameters.size(), models.size(), examples.size(), backgroundColor.orElse(""));
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
