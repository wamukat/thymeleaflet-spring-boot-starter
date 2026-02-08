package io.github.wamukat.thymeleaflet.domain.service;

import io.github.wamukat.thymeleaflet.domain.service.DocumentationAnalysisService;
import io.github.wamukat.thymeleaflet.domain.service.StoryDataRepository;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.TypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ストーリーパラメータドメインサービス
 * 
 * Clean Architecture に従い、Infrastructure依存を排除したドメインロジックを提供
 * Infrastructure依存はPortパターンで抽象化
 */
@Service
public class StoryParameterDomainService {
    
    private static final Logger logger = LoggerFactory.getLogger(StoryParameterDomainService.class);
    
    private final StoryDataRepository storyDataRepository;
    private final DocumentationAnalysisService documentationAnalysisService;
    
    public StoryParameterDomainService(StoryDataRepository storyDataRepository, 
                                     DocumentationAnalysisService documentationAnalysisService) {
        this.storyDataRepository = storyDataRepository;
        this.documentationAnalysisService = documentationAnalysisService;
    }
    
    /**
     * ストーリー用パラメータを生成
     */
    public Map<String, Object> generateStoryParameters(FragmentStoryInfo storyInfo) {
        Map<String, Object> parameters = new HashMap<>();
        
        try {
            // 1. まず、FragmentStoryInfoに既にパラメータが含まれているかチェック
            Map<String, Object> existingParams = storyInfo.parameters();
            if (!existingParams.isEmpty()) {
                logger.debug("Using existing story parameters from FragmentStoryInfo: {}", existingParams);
                return new HashMap<>(existingParams);
            }
            
            // 2. stories.ymlからストーリー固有のパラメータをロード
            Map<String, Object> storySpecificParams = storyDataRepository.loadStoryParameters(storyInfo);
            if (!storySpecificParams.isEmpty()) {
                logger.debug("Loaded story parameters from stories.yml: {}", storySpecificParams);
                return storySpecificParams;
            }
            
            // 3. パラメータが無い場合のみ、型情報からデフォルト値を生成
            List<TypeInfo> typeInfos = documentationAnalysisService.extractTypeInformation(
                storyInfo.getFragmentSummary().getTemplatePath());
            
            for (String paramName : storyInfo.getFragmentSummary().getParameters()) {
                Optional<TypeInfo> typeInfo = findTypeInfoByName(typeInfos, paramName);
                Object parameterValue = generateParameterValue(paramName, typeInfo);
                
                if (parameterValue != null) {
                    parameters.put(paramName, parameterValue);
                    logger.debug("Generated default parameter: {}={}", paramName, parameterValue);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to generate story parameters for {}: {}", 
                        storyInfo.getFragmentSummary().getFragmentName(), e.getMessage());
        }
        
        return parameters;
    }
    
    /**
     * パラメータ値を生成
     */
    public Object generateParameterValue(String paramName, Optional<TypeInfo> typeInfo) {
        if (typeInfo.isEmpty()) {
            return generateDefaultValue(paramName);
        }
        TypeInfo resolvedTypeInfo = typeInfo.orElseThrow();

        switch (resolvedTypeInfo.getTypeCategory()) {
            case PRIMITIVE:
                return generatePrimitiveValue(paramName, resolvedTypeInfo);
            case ENUM:
                return generateEnumValue(resolvedTypeInfo);
            case COLLECTION:
                return generateCollectionValue();
            case OBJECT:
                return generateObjectValue(paramName);
            default:
                return generateDefaultValue(paramName);
        }
    }
    
    private Optional<TypeInfo> findTypeInfoByName(List<TypeInfo> typeInfos, String parameterName) {
        return typeInfos.stream()
                .filter(typeInfo -> typeInfo.getParameterName().equals(parameterName))
                .findFirst();
    }
    
    private Object generateDefaultValue(String paramName) {
        // パラメータ名に基づくヒューリスティック値生成
        String lowerName = paramName.toLowerCase();
        
        if (lowerName.contains("name") || lowerName.contains("title")) {
            return "Sample " + capitalizeFirst(paramName);
        }
        if (lowerName.contains("count") || lowerName.contains("size")) {
            return 5;
        }
        if (lowerName.contains("enabled") || lowerName.contains("visible")) {
            return true;
        }
        
        return "value";
    }
    
    private Object generatePrimitiveValue(String paramName, TypeInfo typeInfo) {
        String typeName = typeInfo.getJavaTypeName().toLowerCase();
        
        if (typeName.contains("boolean")) {
            return !paramName.toLowerCase().contains("disabled");
        }
        if (typeName.contains("int") || typeName.contains("number")) {
            if (paramName.toLowerCase().contains("count")) return 3;
            if (paramName.toLowerCase().contains("size")) return 10;
            return 1;
        }
        // Default to string
        if (!typeInfo.getAllowedValues().isEmpty()) {
            return typeInfo.getAllowedValues().get(0);
        }
        return "Sample " + capitalizeFirst(paramName);
    }
    
    private Object generateCollectionValue() {
        List<Object> list = new ArrayList<>();
        list.add("Item 1");
        list.add("Item 2");
        return list;
    }
    
    private Object generateEnumValue(TypeInfo typeInfo) {
        if (!typeInfo.getAllowedValues().isEmpty()) {
            return createPseudoEnum(typeInfo.getAllowedValues().get(0));
        }
        return PseudoEnum.defaultValue();
    }
    
    private Object generateObjectValue(String paramName) {
        Map<String, Object> obj = new HashMap<>();
        obj.put("id", "sample-" + paramName);
        obj.put("name", "Sample " + capitalizeFirst(paramName));
        return obj;
    }
    
    private String capitalizeFirst(String str) {
        if (str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private Object createPseudoEnum(String value) {
        return PseudoEnum.of(value);
    }
    
    /**
     * 疑似Enumオブジェクト - Value Object (不変化版)
     * 
     * テンプレートが.name()メソッドを呼び出せるように
     * Clean Architecture: 不変Value Object設計
     */
    public static class PseudoEnum {
        private final String value;
        
        /**
         * プライベートコンストラクタ - 不変Value Object設計
         * 
         * ファクトリメソッドのみからのインスタンス化を強制
         * Clean Architecture: 検証済み値による安全なオブジェクト生成
         */
        private PseudoEnum(String value) {
            this.value = Objects.requireNonNull(value, "value cannot be null");
        }
        
        /**
         * PseudoEnum作成 - ファクトリメソッド
         */
        public static PseudoEnum of(String value) {
            return new PseudoEnum(value);
        }
        
        /**
         * デフォルト値PseudoEnum作成 - ファクトリメソッド
         */
        public static PseudoEnum defaultValue() {
            return new PseudoEnum("DEFAULT");
        }
        
        public String name() {
            return value;
        }
        
        @Override
        public String toString() {
            return value;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PseudoEnum that = (PseudoEnum) obj;
            return Objects.equals(value, that.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}
