package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import io.github.wamukat.thymeleaflet.infrastructure.web.rendering.ThymeleafFragmentRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ストーリー共通データセットアップ専用サービス
 * 
 * 責務: フラグメント・ストーリー表示に必要な共通データの設定処理
 * StoryPreviewController肥大化問題解決のためのInfrastructure層サービス抽出
 */
@Component
public class StoryCommonDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(StoryCommonDataService.class);
    
    @Autowired
    private StoryParameterUseCase storyParameterUseCase;
    
    @Autowired
    private ThymeleafFragmentRenderer thymeleafFragmentRenderer;
    
    @Autowired
    private StoryRetrievalUseCase storyRetrievalUseCase;
    
    @Autowired
    private JavaDocLookupService javaDocLookupService;

    @Autowired
    private FragmentDependencyService fragmentDependencyService;

    @Autowired
    private StorybookProperties storybookProperties;

    @Autowired
    private PreviewConfigService previewConfigService;
    
    /**
     * フラグメント・ストーリー共通データセットアップ
     * 
     * 以下の処理を実行:
     * - ストーリーパラメータ取得・設定
     * - デフォルトストーリーとの比較データ準備
     * - JavaDoc情報取得
     * 
     * @param templatePath テンプレートパス
     * @param fragmentName フラグメント名
     * @param storyName ストーリー名
     * @param storyInfo ストーリー情報
     * @param model Spring MVCモデル
     */
    public void setupCommonStoryData(String templatePath, String fragmentName, String storyName, 
                                   FragmentStoryInfo storyInfo, Model model) {
        // stories.ymlのmodelを事前にモデルへ注入
        Map<String, Object> storyModel = storyInfo.getModel();
        if (storyModel != null && !storyModel.isEmpty()) {
            for (Map.Entry<String, Object> entry : storyModel.entrySet()) {
                model.addAttribute(entry.getKey(), entry.getValue());
            }
            logger.info("Applied story model values: {}", storyModel.keySet());
        }

        // パラメータを取得
        Map<String, Object> storyParameters = storyParameterUseCase.getParametersForStory(storyInfo);
        
        // フォールバックパラメータをstoryInfoオブジェクトに設定
        if (!storyInfo.hasStoryConfig() && !storyParameters.isEmpty()) {
            storyInfo = storyInfo.withFallbackParameters(storyParameters);
            logger.info("Set fallback parameters to storyInfo: {}", storyParameters.keySet());
        }
        
        // パラメータをモデルに設定し、表示用パラメータを取得
        Map<String, Object> displayParameters = thymeleafFragmentRenderer.configureModelWithStoryParameters(storyParameters, model);

        Map<String, Object> displayModel = storyModel != null ? storyModel : new HashMap<>();

        // defaultストーリーの情報を取得（差異ハイライト用）
        FragmentStoryInfo defaultStory = null;
        Map<String, Object> defaultParameters = new HashMap<>();
        
        if (!storyName.equals("default")) {
            defaultStory = storyRetrievalUseCase.getStory(templatePath, fragmentName, "default");
            if (defaultStory != null) {
                Map<String, Object> defaultStoryParams = storyParameterUseCase.getParametersForStory(defaultStory);
                defaultParameters = defaultStoryParams.entrySet().stream()
                    .filter(entry -> !"__storybook_background".equals(entry.getKey()))
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        }
        
        // JavaDoc情報を取得
        io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer.JavaDocInfo javadocInfo =
            javaDocLookupService.findJavaDocInfo(templatePath, fragmentName);
        
        // モデルに設定
        model.addAttribute("displayParameters", displayParameters);
        model.addAttribute("displayModel", displayModel);
        model.addAttribute("dependentComponents",
            fragmentDependencyService.findDependencies(templatePath, fragmentName));
        model.addAttribute("defaultStory", defaultStory);
        model.addAttribute("defaultParameters", defaultParameters);
        model.addAttribute("javadocInfo", javadocInfo);
        model.addAttribute("previewStylesheets", joinResources(storybookProperties.getResources().getStylesheets()));
        model.addAttribute("previewScripts", joinResources(storybookProperties.getResources().getScripts()));
        previewConfigService.applyPreviewConfig(model);
    }

    private String joinResources(java.util.List<String> resources) {
        if (resources == null || resources.isEmpty()) {
            return "";
        }
        return resources.stream()
            .map(value -> value == null ? "" : value.trim())
            .filter(value -> !value.isEmpty())
            .collect(Collectors.joining(","));
    }
}
