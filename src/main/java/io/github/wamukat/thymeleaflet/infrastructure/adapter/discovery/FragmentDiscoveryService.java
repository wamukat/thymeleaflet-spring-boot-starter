package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
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
import java.util.Optional;
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

    private volatile boolean cacheInitialized;
    private volatile List<FragmentInfo> cachedFragments = List.of();
    
    @Autowired
    private FragmentDomainService fragmentDomainService;
    
    @Autowired
    private ResolvedStorybookConfig storybookConfig;

    @Autowired
    private FragmentSignatureParser fragmentSignatureParser;
    
    /**
     * テンプレートディレクトリから全フラグメントを発見
     */
    public List<FragmentInfo> discoverFragments() {
        logger.debug("[DEBUG_FRAGMENT_PARAMS] Starting fragment discovery process");
        if (storybookConfig.getCache().isEnabled() && cacheInitialized) {
            return cachedFragments;
        }
        List<FragmentInfo> fragments = new ArrayList<>();
        
        try {
            // 複数のテンプレートパスから検索
            List<String> templatePaths = storybookConfig.getResources().getTemplatePaths();
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
        
        if (storybookConfig.getCache().isEnabled()) {
            cachedFragments = Collections.unmodifiableList(new ArrayList<>(fragments));
            cacheInitialized = true;
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
            analyzeFragment(templatePath, fragmentDefinition).ifPresent(fragments::add);
        }
        
        return fragments;
    }
    
    /**
     * フラグメント定義を解析してFragmentInfoを作成
     */
    private Optional<FragmentInfo> analyzeFragment(String templatePath, String fragmentDefinition) {
        logger.debug("[DEBUG_FRAGMENT_PARAMS] Analyzing fragment: template={}, definition={}", templatePath, fragmentDefinition);

        FragmentSignatureParser.ParseResult parseResult = fragmentSignatureParser.parse(fragmentDefinition);
        if (parseResult instanceof FragmentSignatureParser.ParseSuccess parseSuccess) {
            String fragmentName = parseSuccess.fragmentName();
            List<String> parameters = new ArrayList<>(parseSuccess.parameters());
            logger.debug("[DEBUG_FRAGMENT_PARAMS] Parsed by FragmentSignatureParser: name={}, parameters={}", fragmentName, parameters);

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
            return Optional.of(fragmentInfo);
        }

        FragmentSignatureParser.ParseError parseError = (FragmentSignatureParser.ParseError) parseResult;
        SignatureDiagnostic signatureDiagnostic = createSignatureDiagnostic(parseError);
        logger.warn(
            "[FRAGMENT_SIGNATURE_DIAGNOSTIC] severity={} code={} template={} fragmentDefinition={} fallback={} devMessage={}",
            signatureDiagnostic.getSeverity(),
            signatureDiagnostic.getCode(),
            templatePath,
            fragmentDefinition,
            "skipped",
            signatureDiagnostic.getDeveloperMessage()
        );
        return Optional.empty();
    }

    private SignatureDiagnostic createSignatureDiagnostic(FragmentSignatureParser.ParseError parseError) {
        return switch (parseError.code()) {
            case INVALID_SIGNATURE -> new SignatureDiagnostic(
                "INVALID_SIGNATURE",
                "WARNING",
                "Invalid fragment signature. Please check th:fragment syntax.",
                parseError.message()
            );
            case UNSUPPORTED_SYNTAX -> new SignatureDiagnostic(
                "UNSUPPORTED_SYNTAX",
                "WARNING",
                "This fragment signature uses syntax not yet supported in Thymeleaflet UI.",
                parseError.message()
            );
        };
    }
    
    
    /**
     * リソースURIからテンプレートパスを抽出
     */
    private String extractTemplatePath(String resourceUri) {
        // 複数のテンプレートパスから最初に見つかるものを使用
        for (String templatePath : storybookConfig.getResources().getTemplatePaths()) {
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
        private final SignatureDiagnostic signatureDiagnostic;
        
        public FragmentInfo(String templatePath, String fragmentName, List<String> parameters, 
                           FragmentDomainService.FragmentType type, String originalDefinition) {
            this(templatePath, fragmentName, parameters, type, originalDefinition, SignatureDiagnostic.none());
        }

        public FragmentInfo(String templatePath, String fragmentName, List<String> parameters,
                           FragmentDomainService.FragmentType type, String originalDefinition,
                           SignatureDiagnostic signatureDiagnostic) {
            this.templatePath = templatePath;
            this.fragmentName = fragmentName;
            this.parameters = Collections.unmodifiableList(parameters);
            this.type = type;
            this.originalDefinition = originalDefinition;
            this.signatureDiagnostic = signatureDiagnostic;
        }
        
        // Getters
        public String getTemplatePath() { return templatePath; }
        public String getFragmentName() { return fragmentName; }
        public List<String> getParameters() { return parameters; }
        public FragmentDomainService.FragmentType getType() { return type; }
        public String getOriginalDefinition() { return originalDefinition; }
        public SignatureDiagnostic getSignatureDiagnostic() { return signatureDiagnostic; }
        public boolean hasSignatureDiagnostic() { return !signatureDiagnostic.isNone(); }

        @Override
        public String toString() {
            return String.format("Fragment{path='%s', name='%s', type=%s, params=%s}", 
                               templatePath, fragmentName, type, parameters);
        }
    }

    public static class SignatureDiagnostic {
        private static final SignatureDiagnostic NONE = new SignatureDiagnostic("NONE", "INFO", "", "");

        private final String code;
        private final String severity;
        private final String userMessage;
        private final String developerMessage;

        public SignatureDiagnostic(String code, String severity, String userMessage, String developerMessage) {
            this.code = code;
            this.severity = severity;
            this.userMessage = userMessage;
            this.developerMessage = developerMessage;
        }

        public String getCode() { return code; }
        public String getSeverity() { return severity; }
        public String getUserMessage() { return userMessage; }
        public String getDeveloperMessage() { return developerMessage; }
        public boolean isNone() { return this == NONE; }
        public static SignatureDiagnostic none() { return NONE; }
    }
    
}
