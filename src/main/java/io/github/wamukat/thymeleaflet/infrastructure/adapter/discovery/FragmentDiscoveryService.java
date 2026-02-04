package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thymeleafフラグメントを自動発見・解析するサービス
 */
@Service
public class FragmentDiscoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(FragmentDiscoveryService.class);
    
    private static final Pattern FRAGMENT_PATTERN = Pattern.compile(
        "th:fragment\\s*=\\s*[\"']([^\"']+)[\"']"
    );
    
    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    
    @Autowired
    private FragmentDomainService fragmentDomainService;
    
    @Autowired
    private StorybookProperties storybookProperties;
    
    /**
     * テンプレートディレクトリから全フラグメントを発見
     */
    public List<FragmentInfo> discoverFragments() {
        logger.debug("[DEBUG_FRAGMENT_PARAMS] Starting fragment discovery process");
        List<FragmentInfo> fragments = new ArrayList<>();
        
        try {
            // 複数のテンプレートパスから検索
            List<String> templatePaths = storybookProperties.getResources().getTemplatePaths();
            logger.debug("[DEBUG_FRAGMENT_PARAMS] Searching in template paths: {}", templatePaths);
            
            for (String templatePath : templatePaths) {
                String searchPattern = "classpath:" + templatePath + "**/*.html";
                Resource[] resources = resourceResolver.getResources(searchPattern);
                logger.debug("[DEBUG_FRAGMENT_PARAMS] Found {} template resources in path: {}", resources.length, templatePath);
                
                for (Resource resource : resources) {
                    String relativeTemplatePath = extractTemplatePath(resource.getURI().toString());
                    String content;
                    try (var inputStream = resource.getInputStream()) {
                        content = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                    
                    logger.debug("[DEBUG_FRAGMENT_PARAMS] Processing template: {} (URI: {})", relativeTemplatePath, resource.getURI());
                    
                    // Storybook自身のフラグメントは除外
                    if (relativeTemplatePath.startsWith("thymeleaflet/")) {
                        logger.debug("[DEBUG_FRAGMENT_PARAMS] Skipping thymeleaflet internal template: {}", relativeTemplatePath);
                        continue;
                    }
                    
                    List<FragmentInfo> templateFragments = parseFragmentsFromTemplate(relativeTemplatePath, content);
                    logger.debug("[DEBUG_FRAGMENT_PARAMS] Found {} fragments in template: {}", templateFragments.size(), relativeTemplatePath);
                    fragments.addAll(templateFragments);
                }
            }
        } catch (IOException e) {
            logger.error("[DEBUG_FRAGMENT_PARAMS] Fragment discovery failed", e);
            throw new RuntimeException("フラグメント発見に失敗しました", e);
        }
        
            logger.debug("[DEBUG_FRAGMENT_PARAMS] Fragment discovery completed. Total fragments found: {}", fragments.size());
        for (FragmentInfo fragment : fragments) {
            logger.debug("[DEBUG_FRAGMENT_PARAMS] Final fragment: {}", fragment.toString());
        }
        
        return fragments;
    }
    
    /**
     * テンプレートからフラグメント情報を解析
     */
    private List<FragmentInfo> parseFragmentsFromTemplate(String templatePath, String content) {
        List<FragmentInfo> fragments = new ArrayList<>();
        Matcher matcher = FRAGMENT_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String fragmentDefinition = matcher.group(1);
            FragmentInfo fragmentInfo = analyzeFragment(templatePath, fragmentDefinition);
            fragments.add(fragmentInfo);
        }
        
        return fragments;
    }
    
    /**
     * フラグメント定義を解析してFragmentInfoを作成
     */
    private FragmentInfo analyzeFragment(String templatePath, String fragmentDefinition) {
        logger.debug("[DEBUG_FRAGMENT_PARAMS] Analyzing fragment: template={}, definition={}", templatePath, fragmentDefinition);
        
        String fragmentName;
        List<String> parameters = new ArrayList<>();
        
        // パラメータ付きフラグメントの解析: "fragmentName(param1, param2, ...)" / "fragmentName()"
        Pattern paramPattern = Pattern.compile("([^(]+)\\(([^)]*)\\)");
        Matcher paramMatcher = paramPattern.matcher(fragmentDefinition);
        
        logger.debug("[DEBUG_FRAGMENT_PARAMS] Parameter pattern matching against: {}", fragmentDefinition);
        
        if (paramMatcher.matches()) {
            fragmentName = paramMatcher.group(1).trim();
            String paramString = paramMatcher.group(2);
            
            logger.debug("[DEBUG_FRAGMENT_PARAMS] Found parameters in fragment: name={}, paramString={}", fragmentName, paramString);
            
            // パラメータをカンマで分割（空の括弧はパラメータなし扱い）
            if (paramString != null && !paramString.trim().isEmpty()) {
                String[] paramArray = paramString.split(",");
                for (String param : paramArray) {
                    String trimmedParam = param.trim();
                    parameters.add(trimmedParam);
                    logger.debug("[DEBUG_FRAGMENT_PARAMS] Added parameter: {}", trimmedParam);
                }
            } else {
                logger.debug("[DEBUG_FRAGMENT_PARAMS] Empty parameter list detected for fragment: {}", fragmentName);
            }
        } else {
            // パラメータなしフラグメント
            fragmentName = fragmentDefinition.trim();
            logger.debug("[DEBUG_FRAGMENT_PARAMS] No parameters found for fragment: {}", fragmentName);
        }
        
        FragmentDomainService.FragmentType type = fragmentDomainService.determineFragmentType(templatePath, fragmentName, parameters);
        
        FragmentInfo fragmentInfo = new FragmentInfo(
            templatePath,
            fragmentName,
            parameters,
            type,
            fragmentDefinition
        );
        
        logger.debug("[DEBUG_FRAGMENT_PARAMS] Created FragmentInfo: path={}, name={}, params={}, type={}", 
                   templatePath, fragmentName, parameters, type);
        
        return fragmentInfo;
    }
    
    
    /**
     * リソースURIからテンプレートパスを抽出
     */
    private String extractTemplatePath(String resourceUri) {
        // 複数のテンプレートパスから最初に見つかるものを使用
        for (String templatePath : storybookProperties.getResources().getTemplatePaths()) {
            String pathWithoutSlash = templatePath.substring(1); // 先頭の / を除去
            int index = resourceUri.indexOf(pathWithoutSlash);
            if (index != -1) {
                return resourceUri.substring(index + pathWithoutSlash.length()).replace(".html", "");
            }
        }
        
        // フォールバック: templates/ で検索
        int index = resourceUri.indexOf("templates/");
        if (index != -1) {
            return resourceUri.substring(index + "templates/".length()).replace(".html", "");
        }
        
        return resourceUri;
    }
    
    /**
     * フラグメント情報を保持するクラス
     */
    public static class FragmentInfo {
        private final String templatePath;
        private final String fragmentName;
        private final List<String> parameters;
        private final FragmentDomainService.FragmentType type;
        private final String originalDefinition;
        
        public FragmentInfo(String templatePath, String fragmentName, List<String> parameters, 
                           FragmentDomainService.FragmentType type, String originalDefinition) {
            this.templatePath = templatePath;
            this.fragmentName = fragmentName;
            this.parameters = Collections.unmodifiableList(parameters);
            this.type = type;
            this.originalDefinition = originalDefinition;
        }
        
        // Getters
        public String getTemplatePath() { return templatePath; }
        public String getFragmentName() { return fragmentName; }
        public List<String> getParameters() { return parameters; }
        public FragmentDomainService.FragmentType getType() { return type; }
        public String getOriginalDefinition() { return originalDefinition; }

        @Override
        public String toString() {
            return String.format("Fragment{path='%s', name='%s', type=%s, params=%s}", 
                               templatePath, fragmentName, type, parameters);
        }
    }
    
}
