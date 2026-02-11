package io.github.wamukat.thymeleaflet.domain.model;

import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * フラグメント要約情報 - 軽量DTO
 * 
 * Infrastructure境界での効率的データ転送用
 * Clean Architecture準拠:
 * - ドメイン層の軽量Value Object
 * - 不変性の保証
 * - Infrastructure依存なし
 */
public class FragmentSummary {
    
    private final String templatePath;
    private final String fragmentName;
    private final List<String> parameters;
    private final FragmentDomainService.FragmentType type;
    
    /**
     * プライベートコンストラクタ - 不変Value Object設計
     */
    private FragmentSummary(String templatePath, String fragmentName, List<String> parameters, FragmentDomainService.FragmentType type) {
        this.templatePath = Objects.requireNonNull(templatePath, "templatePath cannot be null");
        this.fragmentName = Objects.requireNonNull(fragmentName, "fragmentName cannot be null");
        this.parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters cannot be null"));
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }
    
    /**
     * 基本データからFragmentSummary作成 - ファクトリメソッド
     * Infrastructure境界変換で利用
     */
    public static FragmentSummary of(String templatePath, String fragmentName, 
                                   List<String> parameters, FragmentDomainService.FragmentType type) {
        return new FragmentSummary(templatePath, fragmentName, parameters, type);
    }
    
    /**
     * FragmentからFragmentSummary作成 - ファクトリメソッド
     * Domain境界変換でFragment.javaから軽量データを抽出
     */
    public static FragmentSummary from(Fragment fragment) {
        Objects.requireNonNull(fragment, "fragment cannot be null");
        
        String templatePath = fragment.getTemplatePath().forFilePath();
        String fragmentName = fragment.getName().getValue();
        List<String> parameters = List.copyOf(fragment.getRequiredParameters());
        
        // FragmentTypeを推論（Parameter有無で判定）
        FragmentDomainService.FragmentType type = parameters.isEmpty() ? 
            FragmentDomainService.FragmentType.SIMPLE : 
            FragmentDomainService.FragmentType.PARAMETERIZED;
            
        return new FragmentSummary(templatePath, fragmentName, parameters, type);
    }
    
    /**
     * SimpleフラグメントSummary作成 - ファクトリメソッド
     */
    public static FragmentSummary simple(String templatePath, String fragmentName) {
        return new FragmentSummary(templatePath, fragmentName, Collections.emptyList(), FragmentDomainService.FragmentType.SIMPLE);
    }
    
    /**
     * ParameterizedフラグメントSummary作成 - ファクトリメソッド
     */
    public static FragmentSummary parameterized(String templatePath, String fragmentName, List<String> parameters) {
        return new FragmentSummary(templatePath, fragmentName, parameters, FragmentDomainService.FragmentType.PARAMETERIZED);
    }
    
    public String getTemplatePath() {
        return templatePath;
    }
    
    public String getFragmentName() {
        return fragmentName;
    }
    
    public List<String> getParameters() {
        return parameters;
    }
    
    public FragmentDomainService.FragmentType getType() {
        return type;
    }

    /**
     * View compatibility helper.
     * FragmentSummary does not carry signature diagnostics, so this is always false.
     */
    public boolean hasSignatureDiagnostic() {
        return false;
    }

    /**
     * 互換表示用のフラグメントシグネチャを返す。
     * Infrastructure FragmentInfo が無い場面でも th:fragment 相当の表示に使う。
     */
    public String getOriginalDefinition() {
        if (parameters.isEmpty()) {
            return fragmentName;
        }
        String joined = parameters.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));
        return fragmentName + "(" + joined + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FragmentSummary that = (FragmentSummary) obj;
        return Objects.equals(templatePath, that.templatePath) &&
               Objects.equals(fragmentName, that.fragmentName) &&
               Objects.equals(parameters, that.parameters) &&
               type == that.type;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(templatePath, fragmentName, parameters, type);
    }
    
    @Override
    public String toString() {
        return String.format("FragmentSummary{templatePath='%s', fragmentName='%s', parameters=%s, type=%s}",
                           templatePath, fragmentName, parameters, type);
    }
}
