package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.preview.FragmentPreviewUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * フラグメントJSON変換処理専用サービス
 * 
 * 責務: フラグメントリストの拡張JSON形式変換とModel属性設定
 * StoryPreviewController肥大化問題解決のためのInfrastructure層サービス抽出
 */
@Component
public class FragmentJsonService {
    
    private static final Logger logger = LoggerFactory.getLogger(FragmentJsonService.class);
    
    @Autowired
    private StoryRetrievalUseCase storyRetrievalUseCase;
    
    @Autowired
    private FragmentPreviewUseCase fragmentPreviewUseCase;
    
    @Autowired
    private FragmentSummaryMapper fragmentSummaryMapper;

    @Autowired
    private StoryParameterUseCase storyParameterUseCase;

    /**
     * フラグメントリストを拡張JSON形式に変換してModel属性に設定
     * 型安全性を向上: Object型で受け取り実行時に適切に処理
     * 
     * @param allFragments フラグメント情報リスト (FragmentSummaryまたはFragmentInfo)
     * @param hierarchicalFragments 階層化フラグメント情報
     * @param model Spring MVCモデル
     */
    public void setupFragmentJsonAttributes(Object allFragments, 
                                           Map<String, Object> hierarchicalFragments, 
                                           Model model) {
        // 型チェックと変換処理
        List<FragmentSummary> fragmentSummaryList;
        if (!(allFragments instanceof List<?> fragmentList)) {
            logger.error("allFragments is not a List: {}", classNameOf(allFragments));
            model.addAttribute("fragmentsJson", "[]");
            model.addAttribute("hierarchicalJson", "{}");
            return;
        }
        if (fragmentList.isEmpty()) {
            fragmentSummaryList = Collections.emptyList();
        } else {
            Object firstElement = fragmentList.get(0);
            if (firstElement instanceof FragmentDiscoveryService.FragmentInfo) {
                // FragmentInfo型の場合、FragmentSummaryに変換
                logger.info("Converting FragmentInfo list to FragmentSummary list");
                @SuppressWarnings("unchecked")
                List<FragmentDiscoveryService.FragmentInfo> infraFragments = (List<FragmentDiscoveryService.FragmentInfo>) fragmentList;
                fragmentSummaryList = infraFragments.stream()
                    .map(fragmentSummaryMapper::toDomain)
                    .collect(Collectors.toList());
            } else if (firstElement instanceof FragmentSummary) {
                // 既にFragmentSummary型の場合
                logger.info("Using existing FragmentSummary list");
                @SuppressWarnings("unchecked")
                List<FragmentSummary> summaryList = (List<FragmentSummary>) fragmentList;
                fragmentSummaryList = summaryList;
            } else {
                logger.error("Unsupported fragment type: {}", classNameOf(firstElement));
                model.addAttribute("fragmentsJson", "[]");
                model.addAttribute("hierarchicalJson", "{}");
                return;
            }
        }
        
        // 各フラグメントにストーリー情報を付加
        List<Map<String, Object>> enrichedFragments = fragmentSummaryList.stream()
            .map(fragment -> {
                Map<String, Object> fragmentData = new HashMap<>();
                fragmentData.put("templatePath", fragment.getTemplatePath());
                fragmentData.put("fragmentName", fragment.getFragmentName());
                fragmentData.put("parameters", fragment.getParameters());
                fragmentData.put("type", fragment.getType().name());
                fragmentData.put("originalDefinition", ""); // FragmentSummaryにはoriginalDefinitionなし
                
                // ストーリー情報を取得
                List<FragmentStoryInfo> stories = storyRetrievalUseCase.getStoriesForFragment(fragment);
                fragmentData.put("stories", stories.stream().map(story -> {
                    Map<String, Object> storyData = new HashMap<>();
                    Map<String, Object> storyParameters = story.getParameters();
                    if (storyParameters.isEmpty()) {
                        Map<String, Object> fallbackParameters = storyParameterUseCase.getParametersForStory(story);
                        if (!fallbackParameters.isEmpty()) {
                            storyParameters = fallbackParameters;
                        }
                    }
                    storyData.put("storyName", story.getStoryName());
                    storyData.put("displayTitle", story.getDisplayTitle());
                    storyData.put("displayDescription", story.getDisplayDescription());
                    storyData.put("hasStoryConfig", story.hasStoryConfig());
                    if (!storyParameters.isEmpty()) {
                        Map<String, Object> sanitizedParameters = new HashMap<>();
                        storyParameters.forEach((key, value) -> sanitizedParameters.put(key, sanitizeParameterValue(value)));
                        storyData.put("parameters", sanitizedParameters);
                    } else {
                        storyData.put("parameters", storyParameters);
                    }
                    storyData.put("model", story.getModel());
                    return storyData;
                }).collect(Collectors.toList()));
                
                return fragmentData;
            }).collect(Collectors.toList());
        
        // JSON変換処理 (UseCase経由呼び出し)
        // hierarchicalFragmentsをListに変換
        List<Map<String, Object>> hierarchicalFragmentsList = List.of(hierarchicalFragments);
        fragmentPreviewUseCase.setupFragmentJsonAttributes(enrichedFragments, hierarchicalFragmentsList, model);
    }

    private @Nullable Object sanitizeParameterValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return value;
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> sanitized = new HashMap<>();
            mapValue.forEach((key, entryValue) -> sanitized.put(String.valueOf(key), sanitizeParameterValue(entryValue)));
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> sanitized = new java.util.ArrayList<>();
            iterable.forEach(entryValue -> sanitized.add(sanitizeParameterValue(entryValue)));
            return sanitized;
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> sanitized = new java.util.ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                sanitized.add(sanitizeParameterValue(java.lang.reflect.Array.get(value, i)));
            }
            return sanitized;
        }
        return value.toString();
    }

    private String classNameOf(@Nullable Object target) {
        return Objects.isNull(target) ? "null" : target.getClass().getName();
    }
}
