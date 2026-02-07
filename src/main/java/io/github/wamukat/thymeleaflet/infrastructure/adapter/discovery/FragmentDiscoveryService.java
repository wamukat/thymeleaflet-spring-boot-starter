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

    private volatile List<FragmentInfo> cachedFragments;
    
    @Autowired
    private FragmentDomainService fragmentDomainService;
    
    @Autowired
    private StorybookProperties storybookProperties;

    @Autowired
    private FragmentSignatureParser fragmentSignatureParser;
    
    /**
     * テンプレートディレクトリから全フラグメントを発見
     */
    public List<FragmentInfo> discoverFragments() {
        logger.debug("[DEBUG_FRAGMENT_PARAMS] Starting fragment discovery process");
        if (storybookProperties.getCache().isEnabled() && cachedFragments != null) {
            return cachedFragments;
        }
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
        
        if (storybookProperties.getCache().isEnabled()) {
            cachedFragments = Collections.unmodifiableList(new ArrayList<>(fragments));
            return cachedFragments;
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
        List<String> parameters;

        FragmentSignatureParser.ParseResult parseResult = fragmentSignatureParser.parse(fragmentDefinition);
        if (parseResult.success()) {
            fragmentName = parseResult.fragmentName();
            parameters = new ArrayList<>(parseResult.parameters());
            logger.debug("[DEBUG_FRAGMENT_PARAMS] Parsed by FragmentSignatureParser: name={}, parameters={}", fragmentName, parameters);
        } else {
            // keep backward compatibility: fallback to plain name when signature parse fails
            fragmentName = fragmentDefinition.trim();
            parameters = new ArrayList<>();
            logger.warn(
                "[DEBUG_FRAGMENT_PARAMS] Fragment signature parse failed. fallback=plain-name, definition={}, code={}, message={}",
                fragmentDefinition,
                parseResult.code(),
                parseResult.message()
            );
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
