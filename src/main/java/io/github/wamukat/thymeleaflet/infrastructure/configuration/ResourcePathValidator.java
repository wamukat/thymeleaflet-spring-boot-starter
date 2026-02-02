package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * リソースパスの検証クラス
 * 
 * テンプレートファイルやCSSファイルのパスを検証し、
 * セキュリティ問題（ディレクトリトラバーサル攻撃など）を防ぐ。
 */
@Component
public class ResourcePathValidator {
    
    private final List<String> allowedTemplateBasePaths = List.of(
        "/templates/",
        "/META-INF/resources/templates/",
        "/custom-templates/"
    );
    
    private final List<String> allowedCssBasePaths = List.of(
        "/static/css/",
        "/META-INF/resources/css/",
        "/assets/"
    );
    
    /**
     * CSS パスの検証
     */
    public void validateCssPath(String cssPath) {
        validateResourcePath(cssPath, allowedCssBasePaths, "CSS");
    }
    
    /**
     * テンプレートパスの検証
     */
    public void validateTemplatePath(String templatePath) {
        validateResourcePath(templatePath, allowedTemplateBasePaths, "Template");
    }
    
    /**
     * リソースパスの共通検証ロジック
     */
    private void validateResourcePath(String resourcePath, List<String> allowedBasePaths, String resourceType) {
        // 1. パス正規化
        Path normalizedPath = Paths.get(resourcePath).normalize();
        
        // 2. 親ディレクトリ参照の禁止
        if (normalizedPath.toString().contains("..")) {
            throw new SecurityException(resourceType + " path: Parent directory access not allowed");
        }
        
        // 3. 許可されたベースパス配下の確認
        boolean isAllowed = allowedBasePaths.stream()
            .anyMatch(allowed -> normalizedPath.toString().startsWith(allowed));
            
        if (!isAllowed) {
            throw new SecurityException(resourceType + " path not in allowed directories: " + resourcePath);
        }
        
        // 4. ファイル存在確認
        Resource resource = new ClassPathResource(normalizedPath.toString().substring(1)); // 先頭の / を除去
        if (!resource.exists()) {
            throw new IllegalArgumentException(resourceType + " file not found: " + resourcePath);
        }
    }
    
    /**
     * テンプレートファイルを検索し、存在する最初のリソースを返す
     * 複数のテンプレートパスから順次検索する
     */
    public Resource findTemplate(String templatePath, List<String> templatePaths) {
        for (String basePath : templatePaths) {
            String fullPath = basePath + templatePath + ".html";
            Resource resource = new ClassPathResource(fullPath.substring(1)); // 先頭 / 除去
            if (resource.exists()) {
                return resource;
            }
        }
        throw new IllegalArgumentException("Template not found: " + templatePath);
    }
}