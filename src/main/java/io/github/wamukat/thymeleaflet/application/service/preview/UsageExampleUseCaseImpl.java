package io.github.wamukat.thymeleaflet.application.service.preview;

import io.github.wamukat.thymeleaflet.application.port.inbound.preview.UsageExampleUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 使用例生成専用ユースケース実装
 * 
 * 責務: 使用例生成のみ
 * SRP準拠: 単一責任原則に従い、使用例生成のみを担当
 */
@Component
@Transactional(readOnly = true)
public class UsageExampleUseCaseImpl implements UsageExampleUseCase {

    private static final String PREVIEW_WRAPPER_PLACEHOLDER = "{{content}}";

    @Override
    public UsageExampleResult generateUsageExample(UsageExampleCommand command) {
        // 基本的な使用例生成（将来拡張予定）
        return UsageExampleResult.success();
    }

    @Override
    public UsageExampleResponse generateUsageExample(FragmentStoryInfo storyInfo, Map<String, Object> parameters) {
        // 元の実装を復元: HTMLコメント + div th:replace形式
        StringBuilder example = new StringBuilder();
        example.append("<!-- Parameters used in the preview -->");
        example.append("\n");
        
        if (parameters.isEmpty()) {
            // パラメータなしフラグメント
            example.append(String.format(
                "<div th:replace=\"~{%s :: %s}\"></div>",
                storyInfo.getFragmentSummary().getTemplatePath(),
                storyInfo.getFragmentSummary().getFragmentName()
            ));
        } else {
            // パラメータ付きフラグメント（__storybook_backgroundを除外）
            String paramString = parameters.entrySet().stream()
                .filter(entry -> !"__storybook_background".equals(entry.getKey()))
                .map(entry -> String.format("'%s'", entry.getValue()))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
                
            example.append(String.format(
                "<div th:replace=\"~{%s :: %s(%s)}\"></div>",
                storyInfo.getFragmentSummary().getTemplatePath(),
                storyInfo.getFragmentSummary().getFragmentName(),
                paramString
            ));
        }
        
        String wrappedExample = applyPreviewWrapperIfNeeded(storyInfo, example.toString());
        return UsageExampleResponse.success(
            wrappedExample,
            storyInfo.getFragmentSummary().getFragmentName(),
            storyInfo.getStoryName()
        );
    }

    @Override
    public ErrorUsageExampleResponse generateErrorUsageExample(FragmentStoryInfo storyInfo) {
        // エラー時の使用例生成
        String errorExample = String.format(
            "<!-- Failed to load parameters -->\n<div th:replace=\"~{%s :: %s}\"></div>",
            storyInfo.getFragmentSummary().getTemplatePath(),
            storyInfo.getFragmentSummary().getFragmentName()
        );
        
        return new ErrorUsageExampleResponse(errorExample);
    }

    private String applyPreviewWrapperIfNeeded(FragmentStoryInfo storyInfo, String example) {
        String wrapper = storyInfo.getStory().preview().wrapper();
        if (wrapper.isBlank()) {
            return example;
        }
        int placeholderIndex = wrapper.indexOf(PREVIEW_WRAPPER_PLACEHOLDER);
        if (placeholderIndex < 0) {
            return example;
        }
        return wrapper.replace(PREVIEW_WRAPPER_PLACEHOLDER, example);
    }
}
