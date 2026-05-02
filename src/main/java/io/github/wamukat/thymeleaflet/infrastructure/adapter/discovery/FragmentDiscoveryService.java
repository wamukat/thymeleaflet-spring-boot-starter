package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.domain.service.FragmentExpressionParser;
import io.github.wamukat.thymeleaflet.domain.service.FragmentReferenceAttributes;
import io.github.wamukat.thymeleaflet.domain.service.ParserDiagnostic;
import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;
import io.github.wamukat.thymeleaflet.infrastructure.cache.ThymeleafletCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Thymeleafフラグメントを自動発見・解析するサービス
 */
@Service
public class FragmentDiscoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(FragmentDiscoveryService.class);
    
    private final TemplateScanner templateScanner;
    private final FragmentDefinitionParser fragmentDefinitionParser;
    private final FragmentDomainService fragmentDomainService;
    private final FragmentSignatureParser fragmentSignatureParser;
    private final StructuredTemplateParser structuredTemplateParser;
    private final FragmentExpressionParser fragmentExpressionParser;
    private final ThymeleafletCacheManager cacheManager;

    public FragmentDiscoveryService(
        TemplateScanner templateScanner,
        FragmentDefinitionParser fragmentDefinitionParser,
        FragmentDomainService fragmentDomainService,
        FragmentSignatureParser fragmentSignatureParser,
        StructuredTemplateParser structuredTemplateParser,
        FragmentExpressionParser fragmentExpressionParser,
        ThymeleafletCacheManager cacheManager
    ) {
        this.templateScanner = Objects.requireNonNull(templateScanner, "templateScanner cannot be null");
        this.fragmentDefinitionParser =
            Objects.requireNonNull(fragmentDefinitionParser, "fragmentDefinitionParser cannot be null");
        this.fragmentDomainService = Objects.requireNonNull(fragmentDomainService, "fragmentDomainService cannot be null");
        this.fragmentSignatureParser =
            Objects.requireNonNull(fragmentSignatureParser, "fragmentSignatureParser cannot be null");
        this.structuredTemplateParser =
            Objects.requireNonNull(structuredTemplateParser, "structuredTemplateParser cannot be null");
        this.fragmentExpressionParser =
            Objects.requireNonNull(fragmentExpressionParser, "fragmentExpressionParser cannot be null");
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager cannot be null");
    }
    
    /**
     * テンプレートディレクトリから全フラグメントを発見
     */
    public List<FragmentInfo> discoverFragments() {
        logger.debug("[DEBUG_FRAGMENT_PARAMS] Starting fragment discovery process");
        Optional<List<FragmentInfo>> cached = cacheManager.get("fragment-discovery", "all");
        if (cached.isPresent()) {
            return cached.orElseThrow();
        }
        List<FragmentInfo> fragments = new ArrayList<>();
        
        try {
            for (TemplateScanner.TemplateResource template : templateScanner.scanTemplates()) {
                logger.debug("[DEBUG_FRAGMENT_PARAMS] Processing template: {} (URI: {})", template.templatePath(), template.uri());

                // Storybook自身のフラグメントは除外
                if (template.templatePath().startsWith("thymeleaflet/")) {
                    logger.debug("[DEBUG_FRAGMENT_PARAMS] Skipping thymeleaflet internal template: {}", template.templatePath());
                    continue;
                }

                List<FragmentInfo> templateFragments = parseFragmentsFromTemplate(template);
                logger.debug("[DEBUG_FRAGMENT_PARAMS] Found {} fragments in template: {}", templateFragments.size(), template.templatePath());
                fragments.addAll(templateFragments);
            }
        } catch (IOException e) {
            logger.error("[DEBUG_FRAGMENT_PARAMS] Fragment discovery failed", e);
            throw new RuntimeException("フラグメント発見に失敗しました", e);
        }
        
            logger.debug("[DEBUG_FRAGMENT_PARAMS] Fragment discovery completed. Total fragments found: {}", fragments.size());
        for (FragmentInfo fragment : fragments) {
            logger.debug("[DEBUG_FRAGMENT_PARAMS] Final fragment: {}", fragment.toString());
        }
        
        List<FragmentInfo> immutableFragments = Collections.unmodifiableList(new ArrayList<>(fragments));
        cacheManager.put("fragment-discovery", "all", immutableFragments);
        return immutableFragments;
    }

    public List<ParserDiagnostic> findTemplateParserDiagnostics(String templatePath) {
        try {
            for (TemplateScanner.TemplateResource template : templateScanner.scanTemplates()) {
                if (!template.templatePath().equals(templatePath)) {
                    continue;
                }
                return parserDiagnostics(template.templatePath(), template.content());
            }
        } catch (IOException exception) {
            logger.warn("Failed to scan template diagnostics for {}: {}", templatePath, exception.getMessage());
        }
        return List.of();
    }

    private List<ParserDiagnostic> parserDiagnostics(String templatePath, String templateContent) {
        StructuredTemplateParser.TemplateParseResult parseResult =
            structuredTemplateParser.parseWithDiagnostics(templateContent);
        List<ParserDiagnostic> diagnostics = new ArrayList<>(parseResult.diagnostics());
        for (StructuredTemplateParser.TemplateElement element : parseResult.parsedTemplate().elements()) {
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                if (!attribute.hasValue()
                    || !FragmentReferenceAttributes.isReferenceAttribute(attribute.name())) {
                    continue;
                }
                diagnostics.addAll(
                    fragmentExpressionParser.parseWithDiagnostics(attribute.value(), templatePath).diagnostics()
                );
            }
        }
        return List.copyOf(diagnostics);
    }
    
    /**
     * テンプレートからフラグメント情報を解析
     */
    private List<FragmentInfo> parseFragmentsFromTemplate(TemplateScanner.TemplateResource template) {
        List<FragmentInfo> fragments = new ArrayList<>();
        for (FragmentDefinitionParser.FragmentDefinition definition : fragmentDefinitionParser.parseTemplate(
            template.templatePath(),
            template.content()
        )) {
            analyzeFragment(definition.templatePath(), definition.definition()).ifPresent(fragments::add);
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
