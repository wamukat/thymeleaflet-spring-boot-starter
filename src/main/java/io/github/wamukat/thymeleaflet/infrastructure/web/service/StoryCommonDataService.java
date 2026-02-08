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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        if (!storyModel.isEmpty()) {
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

        Map<String, Object> displayModel = storyModel;

        // defaultストーリーの情報を取得（差異ハイライト用）
        FragmentStoryInfo defaultStory = null;
        Map<String, Object> defaultParameters = new HashMap<>();
        
        if (!storyName.equals("default")) {
            var defaultStoryOptional = storyRetrievalUseCase.getStory(templatePath, fragmentName, "default");
            if (defaultStoryOptional.isPresent()) {
                defaultStory = defaultStoryOptional.orElseThrow();
                Map<String, Object> defaultStoryParams = storyParameterUseCase.getParametersForStory(defaultStory);
                defaultParameters = defaultStoryParams.entrySet().stream()
                    .filter(entry -> !"__storybook_background".equals(entry.getKey()))
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        }
        
        // JavaDoc情報を取得
        Optional<io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer.JavaDocInfo> javadocInfo =
            javaDocLookupService.findJavaDocInfo(templatePath, fragmentName);

        List<String> orderedParameterNames = resolveOrderedParameterNames(storyInfo, displayParameters, javadocInfo);
        Map<String, Object> orderedDisplayParameters = resolveOrderedDisplayParameters(displayParameters, orderedParameterNames);
        
        // モデルに設定
        model.addAttribute("displayParameters", orderedDisplayParameters);
        model.addAttribute("orderedParameterNames", orderedParameterNames);
        model.addAttribute("displayModel", displayModel);
        model.addAttribute("dependentComponents",
            fragmentDependencyService.findDependencies(templatePath, fragmentName));
        model.addAttribute("defaultStory", defaultStory);
        model.addAttribute("defaultParameters", defaultParameters);
        model.addAttribute("javadocInfo", javadocInfo.orElse(null));
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

    private List<String> resolveOrderedParameterNames(
        FragmentStoryInfo storyInfo,
        Map<String, Object> displayParameters,
        Optional<io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer.JavaDocInfo> javadocInfo
    ) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();

        javadocInfo
            .map(io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer.JavaDocInfo::getParameters)
            .ifPresent(parameters -> {
                for (var parameterInfo : parameters) {
                    if (!parameterInfo.getName().isBlank()) {
                        ordered.add(parameterInfo.getName());
                    }
                }
            });

        ordered.addAll(storyInfo.getFragmentSummary().getParameters());

        ordered.addAll(displayParameters.keySet());

        return new ArrayList<>(ordered);
    }

    private Map<String, Object> resolveOrderedDisplayParameters(
        Map<String, Object> displayParameters,
        List<String> orderedParameterNames
    ) {
        Map<String, Object> source = displayParameters;
        LinkedHashMap<String, Object> ordered = new LinkedHashMap<>();

        for (String parameterName : orderedParameterNames) {
            if (source.containsKey(parameterName)) {
                ordered.put(parameterName, source.get(parameterName));
            }
        }

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            ordered.putIfAbsent(entry.getKey(), entry.getValue());
        }

        return ordered;
    }
}
