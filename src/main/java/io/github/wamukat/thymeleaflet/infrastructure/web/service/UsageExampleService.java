package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.ValidationUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.preview.UsageExampleUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.SecureTemplatePath;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.Map;
import java.util.Optional;

/**
 * 使用例生成処理専用サービス
 * 
 * 責務: フラグメント使用例のAPIレスポンス生成処理
 * FragmentApiController肥大化問題解決のためのInfrastructure層サービス抽出
 */
@Component
public class UsageExampleService {
    
    private static final Logger logger = LoggerFactory.getLogger(UsageExampleService.class);
    
    @Autowired
    private ValidationUseCase validationUseCase;
    
    @Autowired
    private StoryRetrievalUseCase storyRetrievalUseCase;
    
    @Autowired
    private StoryParameterUseCase storyParameterUseCase;
    
    @Autowired
    private UsageExampleUseCase usageExampleUseCase;
    
    @Autowired
    private FragmentSummaryMapper fragmentSummaryMapper;
    
    /**
     * 使用例生成処理
     * 
     * フラグメントのストーリーに基づいて実際の使用例を生成し、
     * モデルに必要な属性を設定
     * 
     * @param templatePath テンプレートパス (エンコード済み)
     * @param fragmentName フラグメント名
     * @param storyName ストーリー名
     * @param model Spring MVCモデル
     * @return 処理結果
     */
    public UsageExampleResult generateUsageExample(String templatePath, String fragmentName, 
                                                  String storyName, Model model) {
        logger.info("=== UsageExampleService.generateUsageExample START ===");
        logger.info("RAW Parameters: templatePath={}, fragmentName={}, storyName={}", templatePath, fragmentName, storyName);
        
        try {
            // セキュアパス変換を使用してテンプレートパスを復元
            String fullTemplatePath;
            try {
                logger.info("[PATH_CONVERSION] Before conversion: {}", templatePath);
                SecureTemplatePath secureTemplatePath = SecureTemplatePath.of(templatePath);
                fullTemplatePath = secureTemplatePath.forFilePath();
                logger.info("[PATH_CONVERSION] After conversion: {}", fullTemplatePath);
            } catch (Exception e) {
                logger.error("Security violation in template path conversion: {}", e.getMessage());
                return UsageExampleResult.failure("不正なテンプレートパスです: " + e.getMessage());
            }
            
            // ストーリーリクエストのバリデーション
            ValidationUseCase.ValidationCommand requestValidationCommand = 
                new ValidationUseCase.ValidationCommand(
                    fullTemplatePath,
                    fragmentName,
                    storyName
                );
            validationUseCase.validateStoryRequest(requestValidationCommand);
            
            // 対象ストーリーを取得
            Optional<FragmentStoryInfo> storyInfoOptional = storyRetrievalUseCase
                .getStory(fullTemplatePath, fragmentName, storyName);

            if (storyInfoOptional.isEmpty()) {
                model.addAttribute("hasError", true);
                model.addAttribute("errorMessage", "ストーリーが見つかりません");
                model.addAttribute("usageExample", "<!-- ストーリーが見つかりません -->");
                return UsageExampleResult.success();
            }
            FragmentStoryInfo storyInfo = storyInfoOptional.orElseThrow();
            
            // パラメータを取得（JavaDocフォールバックを含む）
            Map<String, Object> storyParameters = storyParameterUseCase.getParametersForStory(storyInfo);
            Map<String, Object> storyModel = storyInfo.getModel();
            
            // SIMPLEフラグメントの場合、パラメータが空でも正常
            boolean isSimpleFragment = storyInfo.getFragmentSummary().getType() == FragmentDomainService.FragmentType.SIMPLE;
            boolean hasRequiredParams = !storyInfo.getFragmentSummary().getParameters().isEmpty();
            boolean hasModel = !storyModel.isEmpty();
            boolean shouldShowError = storyParameters.isEmpty() && hasRequiredParams && !hasModel;
            
            // デバッグログ追加
            logger.info("=== Usage Example Debug ===");
            logger.info("Fragment: {}::{}", storyInfo.getFragmentSummary().getTemplatePath(), storyInfo.getFragmentSummary().getFragmentName());
            logger.info("Fragment Type: {}", storyInfo.getFragmentSummary().getType());
            logger.info("Is Simple Fragment: {}", isSimpleFragment);
            logger.info("Story Parameters: {}", storyParameters);
            logger.info("Parameters Empty: {}", storyParameters.isEmpty());
            logger.info("Should Show Error: {}", shouldShowError);
            
            if (shouldShowError) {
                // PARAMETERIZEDフラグメントでパラメータが取得できない場合のみエラー
                UsageExampleUseCase.ErrorUsageExampleResponse errorResponse = 
                    usageExampleUseCase.generateErrorUsageExample(storyInfo);
                String errorUsageExample = errorResponse.getErrorUsageExample();
                
                model.addAttribute("hasError", true);
                model.addAttribute("errorMessage", "パラメータが取得できません");
                model.addAttribute("usageExample", errorUsageExample);
                model.addAttribute("description", "このフラグメントのパラメータが定義されていません");
            } else {
                // 正常な使用例を生成（SIMPLEフラグメントまたはパラメータ付きフラグメント）
                model.addAttribute("hasError", false);
                
                Model tempModel = new ExtendedModelMap();
                for (Map.Entry<String, Object> entry : storyParameters.entrySet()) {
                    tempModel.addAttribute(entry.getKey(), entry.getValue());
                }
                // モックデータセットアップ
                validationUseCase.setupFragmentValidationData(new ValidationUseCase.ValidationCommand(
                    storyInfo.getFragmentSummary().getTemplatePath(),
                    storyInfo.getFragmentSummary().getFragmentName(),
                    storyInfo.getStoryName()
                ));
                
                // パラメータ抽出（Domain FragmentSummaryをInfrastructure型に変換）
                FragmentDiscoveryService.FragmentInfo infrastructureFragmentInfo = 
                    fragmentSummaryMapper.toInfrastructure(storyInfo.getFragmentSummary());
                StoryParameterUseCase.ParameterExtractionApplicationCommand extractionCommand = 
                    new StoryParameterUseCase.ParameterExtractionApplicationCommand(
                        infrastructureFragmentInfo,
                        tempModel.asMap()
                    );
                StoryParameterUseCase.ParameterExtractionResponse extractionResponse = 
                    storyParameterUseCase.extractRelevantParameters(extractionCommand);
                Map<String, Object> relevantParams = extractionResponse.getParameters();
                    
                // 正常な使用例生成
                UsageExampleUseCase.UsageExampleResponse usageResponse = 
                    usageExampleUseCase.generateUsageExample(storyInfo, relevantParams);
                String rawUsageExample = usageResponse.getUsageExample();
                
                model.addAttribute("usageExample", rawUsageExample);
                model.addAttribute("parameters", relevantParams);
                
                String description;
                if (isSimpleFragment) {
                    description = "パラメータなしのシンプルなフラグメントです";
                } else {
                    description = storyInfo.hasStoryConfig() 
                        ? "プレビューで実際に使用されているパラメータと同じ値です" 
                        : "JavaDocから自動生成されたパラメータです";
                }
                model.addAttribute("description", description);
            }
            
            logger.info("=== UsageExampleService.generateUsageExample COMPLETED ===");
            return UsageExampleResult.success();
            
        } catch (Exception e) {
            logger.error("Usage example generation failed", e);
            return UsageExampleResult.failure("使用例生成に失敗しました: " + e.getMessage());
        }
    }
    
    /**
     * 使用例生成処理結果
     */
    public static class UsageExampleResult {
        private final boolean succeeded;
        private final Optional<String> errorMessage;
        
        private UsageExampleResult(boolean succeeded, Optional<String> errorMessage) {
            this.succeeded = succeeded;
            this.errorMessage = errorMessage;
        }
        
        public static UsageExampleResult success() {
            return new UsageExampleResult(true, Optional.empty());
        }
        
        public static UsageExampleResult failure(String errorMessage) {
            return new UsageExampleResult(false, Optional.of(errorMessage));
        }
        
        public boolean succeeded() { return succeeded; }
        public Optional<String> errorMessage() { return errorMessage; }
    }
}
