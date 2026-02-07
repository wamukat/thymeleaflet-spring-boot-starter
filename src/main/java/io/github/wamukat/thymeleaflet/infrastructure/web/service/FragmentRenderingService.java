package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.ValidationUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.SecureTemplatePath;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.web.rendering.ThymeleafFragmentRenderer;
import io.github.wamukat.thymeleaflet.infrastructure.web.service.SecurePathConversionService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * フラグメント動的レンダリング処理専用サービス
 * 
 * 責務: Thymeleafフラグメントの動的レンダリング詳細処理
 * FragmentRenderingController肥大化問題解決のためのInfrastructure層サービス抽出
 */
@Component
public class FragmentRenderingService {
    
    private static final Logger logger = LoggerFactory.getLogger(FragmentRenderingService.class);
    
    @Autowired
    private ValidationUseCase validationUseCase;
    
    @Autowired
    private StoryRetrievalUseCase storyRetrievalUseCase;
    
    @Autowired
    private StoryParameterUseCase storyParameterUseCase;
    
    @Autowired
    private SecurePathConversionService securePathConversionService;

    @Autowired
    private ThymeleafFragmentRenderer thymeleafFragmentRenderer;
    
    /**
     * ストーリー動的レンダリング処理
     * 
     * セキュリティ変換、ストーリー取得、パラメータ設定、テンプレート参照生成を実行
     * 
     * @param templatePath テンプレートパス (エンコード済み)
     * @param fragmentName フラグメント名
     * @param storyName ストーリー名
     * @param model Spring MVCモデル
     * @return レンダリング結果
     */
    public RenderingResult renderStory(String templatePath, String fragmentName, String storyName, Model model) {
        return renderStory(templatePath, fragmentName, storyName, model, null, null);
    }

    public RenderingResult renderStory(String templatePath,
                                       String fragmentName,
                                       String storyName,
                                       Model model,
                                       @Nullable Map<String, Object> parameterOverrides,
                                       @Nullable Map<String, Object> modelOverrides) {
        try {
            logger.info("=== RENDER STORY START ===");
            logger.info("Request params: templatePath={}, fragmentName={}, storyName={}", 
                       templatePath, fragmentName, storyName);
            
            // セキュアパス変換を使用してテンプレートパスを復元
            SecurePathConversionService.SecurityConversionResult conversionResult = 
                securePathConversionService.convertSecurePath(templatePath, model);
            if (!conversionResult.succeeded()) {
                return RenderingResult.error(conversionResult.templateReference());
            }
            String fullTemplatePath = Objects.requireNonNull(conversionResult.fullTemplatePath());
            logger.info("Full template path: {}", fullTemplatePath);
        
            // 対象ストーリーを取得
            Optional<FragmentStoryInfo> storyInfoOptional = storyRetrievalUseCase
                .getStory(fullTemplatePath, fragmentName, storyName);
            
            logger.info("=== Story Config Debug ===");
            logger.info("Story Info: {}", storyInfoOptional.orElse(null));
            
            if (storyInfoOptional.isEmpty()) {
                logger.info("Story info is null, returning error");
                return RenderingResult.error("thymeleaflet/fragments/error-display :: error(type='info', title=null, message=null, showActionButton=true, actionText=null, actionScript=null, templatePath=null)");
            }
            FragmentStoryInfo storyInfo = storyInfoOptional.orElseThrow();
            
            logger.info("Has Story Config: {}", storyInfo.hasStoryConfig());
            logger.info("Fragment Type: {}", storyInfo.getFragmentSummary().getType());

            Map<String, Object> storyModel = storyInfo.getModel();
            Map<String, Object> mergedModel = new HashMap<>();
            if (storyModel != null && !storyModel.isEmpty()) {
                mergedModel.putAll(storyModel);
            }
            if (modelOverrides != null && !modelOverrides.isEmpty()) {
                mergedModel.putAll(modelOverrides);
            }
            if (!mergedModel.isEmpty()) {
                for (Map.Entry<String, Object> entry : mergedModel.entrySet()) {
                    model.addAttribute(entry.getKey(), thymeleafFragmentRenderer.resolveTemplateValue(entry.getValue()));
                }
                logger.info("Applied story model values: {}", mergedModel.keySet());
            }
            
            // SIMPLEフラグメント（パラメータなし）の場合はstories.ymlは不要
            if (storyInfo.getFragmentSummary().getType() == FragmentDomainService.FragmentType.SIMPLE) {
                logger.info("SIMPLE fragment detected, skipping stories.yml requirement");
                
                // クライアントサイド色判定のため、SIMPLEフラグメントも直接レンダリング
                String templateRef = String.format("%s :: %s", 
                    storyInfo.getFragmentSummary().getTemplatePath(), 
                    storyInfo.getFragmentSummary().getFragmentName());
                logger.info("Rendering SIMPLE fragment with template reference: {}", templateRef);
                return RenderingResult.success(templateRef);
            }

            // DATA_DEPENDENTでもパラメータが無いフラグメントは直接レンダリングを許可
            if (storyInfo.getFragmentSummary().getParameters().isEmpty()) {
                String templateRef = String.format("%s :: %s",
                    storyInfo.getFragmentSummary().getTemplatePath(),
                    storyInfo.getFragmentSummary().getFragmentName());
                logger.info("No-parameter fragment detected (type: {}), rendering directly: {}",
                    storyInfo.getFragmentSummary().getType(), templateRef);
                return RenderingResult.success(templateRef);
            }
            
            // PARAMETERIZEDフラグメントの場合、ストーリー設定またはJavaDocフォールバックを試行
            Map<String, Object> parameters = storyParameterUseCase.getParametersForStory(storyInfo);
            if (parameters == null) {
                parameters = Map.of();
            }
            Map<String, Object> mergedParameters = new HashMap<>(parameters);
            if (parameterOverrides != null && !parameterOverrides.isEmpty()) {
                mergedParameters.putAll(parameterOverrides);
            }
            
            if (mergedParameters.isEmpty() && mergedModel.isEmpty()) {
                // Stories YAMLファイルもJavaDocも利用できない場合はエラー表示
                model.addAttribute("fragmentName", fragmentName);
                model.addAttribute("templatePath", fullTemplatePath);
                logger.info("No parameters available from stories.yml or JavaDoc fallback");
                return RenderingResult.error("thymeleaflet/fragments/error-display :: error(type='warning', title=null, message=null, showActionButton=false, actionText=null, actionScript=null, templatePath=null)");
            }
            
            logger.info("Parameters obtained: {} (from {})", mergedParameters, 
                       storyInfo.hasStoryConfig() ? "stories.yml" : "JavaDoc fallback");
            
            // ストーリーのパラメータを設定
            logger.info("=== PARAMETER SETTING START ===");
            for (Map.Entry<String, Object> entry : mergedParameters.entrySet()) {
                logger.info("Setting parameter: {} = {} (type: {})", 
                           entry.getKey(), entry.getValue(), 
                           entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null");
                model.addAttribute(entry.getKey(), thymeleafFragmentRenderer.resolveTemplateValue(entry.getValue()));
            }
            
            // 追加のモックデータを設定
            logger.info("=== MOCK DATA SETUP ===");
            validationUseCase.setupFragmentValidationData(new ValidationUseCase.ValidationCommand(
                storyInfo.getFragmentSummary().getTemplatePath(),
                storyInfo.getFragmentSummary().getFragmentName(),
                storyInfo.getStoryName()
            ));
            
            // デバッグログ
            String templateRef = String.format("%s :: %s", 
                storyInfo.getFragmentSummary().getTemplatePath(), 
                storyInfo.getFragmentSummary().getFragmentName());
            logger.info("=== Fragment Render Debug ===");
            logger.info("Template Reference: {}", templateRef);
            logger.info("Fragment Name: {}", storyInfo.getFragmentSummary().getFragmentName());
            logger.info("Template Path: {}", storyInfo.getFragmentSummary().getTemplatePath());
            logger.info("Story Name: {}", storyName);
            logger.info("Model attributes: {}", model.asMap().keySet());
            logger.info("All model values: {}", model.asMap());
            
            try {
                logger.info("=== TEMPLATE RENDERING ATTEMPT ===");
                logger.info("About to render template: {}", templateRef);
                
                // クライアントサイド色判定のため、直接フラグメントをレンダリング
                logger.info("Template rendering successful, returning: {}", templateRef);
                return RenderingResult.success(templateRef);
            } catch (Exception e) {
                logger.error("=== TEMPLATE RENDERING FAILED ===");
                logger.error("Template Reference: {}", templateRef);
                logger.error("Exception: {}", e.getMessage(), e);
                
                // エラー時は適切なエラーメッセージを表示
                model.addAttribute("fragmentName", fragmentName);
                model.addAttribute("templatePath", fullTemplatePath);
                model.addAttribute("errorMessage", e.getMessage());
                return RenderingResult.error("thymeleaflet/fragments/error-display :: error(type='warning', title=null, message=null, showActionButton=false, actionText=null, actionScript=null, templatePath=null)");
            }
            
        } catch (Exception globalException) {
            // エラーハンドリング（簡素化版）
            logger.error("Rendering error for {}::{}::{}: {}", 
                templatePath, fragmentName, storyName, globalException.getMessage(), globalException);
            
            model.addAttribute("error", "レンダリングエラーが発生しました: " + globalException.getMessage());
            model.addAttribute("templatePath", templatePath);
            model.addAttribute("fragmentName", fragmentName);
            model.addAttribute("storyName", storyName);
            
            return RenderingResult.error("thymeleaflet/fragments/error-display :: error(type='danger')");
        }
    }

    /**
     * レンダリング処理結果
     */
    public static class RenderingResult {
        private final boolean succeeded;
        private final @Nullable String templateReference;
        
        private RenderingResult(boolean succeeded, @Nullable String templateReference) {
            this.succeeded = succeeded;
            this.templateReference = templateReference;
        }
        
        public static RenderingResult success(String templateReference) {
            return new RenderingResult(true, templateReference);
        }
        
        public static RenderingResult error(@Nullable String templateReference) {
            return new RenderingResult(false, templateReference);
        }
        
        public boolean succeeded() { return succeeded; }
        public @Nullable String templateReference() { return templateReference; }
    }
}
