package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 型情報抽出Infrastructure実装
 * 
 * Phase 5.1: JavaDocTypeExtractorからInfrastructure層への移行
 * Pure Infrastructure責任: 型解析技術的処理のみ
 */
@Component
public class TypeInformationExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(TypeInformationExtractor.class);
    
    // Infrastructure技術的制約: 既知のEnum型パターン定義
    private static final Set<String> KNOWN_ENUM_PATTERNS = Set.of(
        "TransactionType", "Status", "State", "Kind", "Mode", "Level"
    );
    
    // Infrastructure技術的制約: Enum値パターン（ALL_CAPS）
    private static final Pattern ENUM_VALUE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

    private final JavaDocAnalyzer javaDocAnalyzer;

    public TypeInformationExtractor(JavaDocAnalyzer javaDocAnalyzer) {
        this.javaDocAnalyzer = javaDocAnalyzer;
    }

    /**
     * HTMLテンプレートから型情報を技術的抽出
     * Infrastructure責任: 型推論・解析処理
     */
    public List<TypeInfo> extractTypeInformationFromHtml(String htmlContent) {
        logger.debug("Starting type information extraction from HTML content");
        
        List<TypeInfo> typeInfos = new ArrayList<>();
        
        try {
            // JavaDoc解析結果から型情報を抽出
            List<JavaDocAnalyzer.JavaDocInfo> javadocInfos = javaDocAnalyzer.analyzeJavaDocFromHtml(htmlContent);
            logger.debug("Analyzed {} JavaDoc blocks for type extraction", javadocInfos.size());
            
            for (JavaDocAnalyzer.JavaDocInfo javadocInfo : javadocInfos) {
                List<TypeInfo> extractedTypes = extractTypeInfoFromJavaDocAnalysis(javadocInfo);
                typeInfos.addAll(extractedTypes);
                logger.debug("Extracted {} type infos from JavaDoc: {}", extractedTypes.size(), javadocInfo.getDescription());
            }
            
            logger.info("Successfully extracted {} type infos from HTML content", typeInfos.size());
            
        } catch (Exception e) {
            logger.error("Failed to extract type information from HTML: {}", e.getMessage(), e);
        }
        
        return typeInfos;
    }

    /**
     * 単一のJavaDoc解析結果から型情報を技術的抽出
     * Infrastructure責任: パラメータ型推論
     */
    private List<TypeInfo> extractTypeInfoFromJavaDocAnalysis(JavaDocAnalyzer.JavaDocInfo javadocInfo) {
        List<TypeInfo> typeInfos = new ArrayList<>();
        
        for (JavaDocAnalyzer.ParameterInfo paramInfo : javadocInfo.getParameters()) {
            TypeInfo typeInfo = analyzeParameterType(paramInfo, javadocInfo);
            typeInfos.add(typeInfo);
            
            logger.debug("Created TypeInfo for parameter '{}': type='{}', category={}, level={}", 
                       typeInfo.getParameterName(), 
                       typeInfo.getJavaTypeName(),
                       typeInfo.getTypeCategory(),
                       typeInfo.getInferenceLevel());
        }
        
        return typeInfos;
    }

    /**
     * パラメータ情報から型推論を実行
     * Infrastructure技術的責任: 型カテゴリ判定・信頼度計算
     */
    private TypeInfo analyzeParameterType(JavaDocAnalyzer.ParameterInfo paramInfo, JavaDocAnalyzer.JavaDocInfo javadocInfo) {
        String paramName = paramInfo.getName();
        String paramType = paramInfo.getType();
        @Nullable String description = paramInfo.getDescription();
        
        logger.debug("Analyzing parameter type: name='{}', type='{}', description='{}'", paramName, paramType, description);
        
        // 型カテゴリ判定
        TypeInfo.TypeCategory category = determineTypeCategory(paramType, description);
        
        // 推論レベル判定
        TypeInfo.InferenceLevel inferenceLevel = determineInferenceLevel(paramType, category, description);
        
        // Enum型の場合は許可値を抽出
        List<String> allowedValues = new ArrayList<>();
        if (category == TypeInfo.TypeCategory.ENUM) {
            // まずParameterInfoのallowedValuesを使用
            allowedValues.addAll(paramInfo.getAllowedValues());
            // 追加で説明文からも抽出
            if (allowedValues.isEmpty()) {
                allowedValues = extractEnumValues(paramType, description, javadocInfo);
            }
        }
        
        return new TypeInfo.Builder()
            .parameterName(paramName)
            .javaTypeName(paramType)
            .typeCategory(category)
            .inferenceLevel(inferenceLevel)
            .allowedValues(allowedValues)
            .description(description != null ? description : "")
            .build();
    }

    /**
     * 型カテゴリの技術的判定
     * Infrastructure責任: 型分類アルゴリズム
     */
    private TypeInfo.TypeCategory determineTypeCategory(String paramType, @Nullable String description) {
        // プリミティブ型判定
        if (isPrimitiveType(paramType)) {
            return TypeInfo.TypeCategory.PRIMITIVE;
        }
        
        // Collection型判定（Enum判定より先に行う）
        if (isCollectionType(paramType)) {
            return TypeInfo.TypeCategory.COLLECTION;
        }
        
        // Enum型判定（型名またはEnum値パターンで判定）
        if (isEnumType(paramType, description)) {
            return TypeInfo.TypeCategory.ENUM;
        }
        
        // Object型判定
        if (isObjectType(paramType)) {
            return TypeInfo.TypeCategory.OBJECT;
        }
        
        return TypeInfo.TypeCategory.UNKNOWN;
    }

    /**
     * 推論レベルの技術的判定
     * Infrastructure責任: 信頼度アルゴリズム
     */
    private TypeInfo.InferenceLevel determineInferenceLevel(String paramType, TypeInfo.TypeCategory category, @Nullable String description) {
        // 既知のパターンによる高信頼度判定
        if (category == TypeInfo.TypeCategory.PRIMITIVE && isPrimitiveType(paramType)) {
            return TypeInfo.InferenceLevel.EXPLICIT;
        }
        
        // Enum型で既知パターンの場合
        if (category == TypeInfo.TypeCategory.ENUM && KNOWN_ENUM_PATTERNS.contains(paramType)) {
            return TypeInfo.InferenceLevel.INFERRED_FROM_CONTEXT;
        }
        
        // 説明文からの推論
        if (description != null && containsTypeHints(description)) {
            return TypeInfo.InferenceLevel.INFERRED_FROM_CONTEXT;
        }
        
        return TypeInfo.InferenceLevel.FALLBACK;
    }

    /**
     * Enum値の技術的抽出
     * Infrastructure責任: パターンマッチング・値抽出
     */
    private List<String> extractEnumValues(String paramType, @Nullable String description, JavaDocAnalyzer.JavaDocInfo javadocInfo) {
        List<String> values = new ArrayList<>();
        
        // TransactionType特別処理
        if ("TransactionType".equals(paramType)) {
            values.addAll(Arrays.asList("EARN", "USE"));
            return values;
        }
        
        // 説明文からEnum値を抽出（簡易実装）
        if (description != null) {
            String[] words = description.split("\\s+");
            for (String word : words) {
                if (ENUM_VALUE_PATTERN.matcher(word).matches()) {
                    values.add(word);
                }
            }
        }
        
        // 使用例からの値抽出も可能（今回は省略）
        
        return values;
    }

    /**
     * プリミティブ型判定
     * Infrastructure技術的制約チェック
     */
    private boolean isPrimitiveType(String type) {
        return Set.of("String", "Integer", "int", "Boolean", "boolean", "Long", "long", "Double", "double", "Float", "float").contains(type);
    }

    /**
     * Enum型判定
     * Infrastructure技術的パターンマッチング
     */
    private boolean isEnumType(String type, @Nullable String description) {
        // 既知のEnum型名
        if (KNOWN_ENUM_PATTERNS.contains(type)) {
            return true;
        }
        
        // 型名にEnumの特徴があるか
        if (type.endsWith("Type") || type.endsWith("Status") || type.endsWith("Mode")) {
            return true;
        }
        
        // 説明文にEnum値らしきものがあるか
        if (description != null) {
            return ENUM_VALUE_PATTERN.matcher(description).find();
        }
        
        return false;
    }

    /**
     * Collection型判定
     * Infrastructure技術的型チェック
     */
    private boolean isCollectionType(String type) {
        // 配列型の場合はEnum配列の可能性があるので詳細チェック
        if (type.endsWith("[]")) {
            String baseType = type.substring(0, type.length() - 2);
            // Enum型の配列はEnumとして扱う（Collection型ではない）
            if (isEnumType(baseType, null)) {
                return false;
            }
            return true;
        }
        
        return type.startsWith("List<") || type.startsWith("Set<") || type.startsWith("Collection<");
    }

    /**
     * Object型判定
     * Infrastructure技術的型分類
     */
    private boolean isObjectType(String type) {
        // 基本的にはその他すべて（primitive、enum、collection以外）
        return !isPrimitiveType(type) && !type.equals("Object");
    }

    /**
     * 説明文内の型ヒント検出
     * Infrastructure技術的テキスト解析
     */
    private boolean containsTypeHints(String description) {
        String lower = description.toLowerCase();
        return lower.contains("enum") || 
               lower.contains("type") || 
               lower.contains("値") || 
               lower.contains("選択");
    }

    /**
     * 名前による型情報検索
     * Infrastructure技術的検索処理
     */
    public Optional<TypeInfo> findTypeInfoByName(List<TypeInfo> typeInfos, String parameterName) {
        return typeInfos.stream()
            .filter(typeInfo -> typeInfo.getParameterName().equals(parameterName))
            .findFirst();
    }
}
