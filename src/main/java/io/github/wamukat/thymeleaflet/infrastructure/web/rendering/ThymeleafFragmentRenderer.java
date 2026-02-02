package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ExpressionContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thymeleafテンプレートレンダリング専用Infrastructure実装
 * <p>
 * Phase 5.1: FragmentPreviewServiceからの技術的責任抽出
 * Pure Infrastructure責任: テンプレートレンダリング技術的処理のみ
 */
@Component
public class ThymeleafFragmentRenderer {

    private static final Logger logger = LoggerFactory.getLogger(ThymeleafFragmentRenderer.class);

    private final SpringTemplateEngine templateEngine;

    public ThymeleafFragmentRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * 指定されたテンプレートパスとフラグメント名に一致するフラグメントを検索
     * Infrastructure責任: フラグメント選択の技術的処理
     */
    public FragmentDiscoveryService.FragmentInfo findFragmentByIdentifier(
            List<FragmentDiscoveryService.FragmentInfo> allFragments,
            String templatePath,
            String fragmentName) {

        long selectionStart = System.currentTimeMillis();

        FragmentDiscoveryService.FragmentInfo selectedFragment = allFragments.stream()
                .filter(f -> f.getTemplatePath().equals(templatePath) &&
                             f.getFragmentName().equals(fragmentName))
                .findFirst()
                .orElse(null);

        logger.debug("Fragment selection took {} ms, selected: {}",
                System.currentTimeMillis() - selectionStart,
                selectedFragment != null ? selectedFragment.getFragmentName() : "null");

        return selectedFragment;
    }

    /**
     * ストーリーパラメータをThymeleafモデルに設定し、表示用パラメータを返す
     * Infrastructure責任: Thymeleaf固有のモデル操作
     */
    public Map<String, Object> configureModelWithStoryParameters(
            Map<String, Object> storyParameters,
            Model model) {

        IEngineConfiguration configuration = templateEngine.getConfiguration();
        ExpressionContext expressionContext = new ExpressionContext(configuration);

        // Thymeleaf技術的制約: __storybook_backgroundを除外してモデルに設定
        storyParameters.entrySet().stream()
                .filter(entry -> !"__storybook_background".equals(entry.getKey()))
                .forEach(entry -> model.addAttribute(entry.getKey(),
                    toTemplateValue(entry.getValue(), configuration, expressionContext)));

        // 表示用パラメータリスト（Thymeleaf表示制約対応）
        Map<String, Object> displayParameters = storyParameters.entrySet().stream()
                .filter(entry -> !"__storybook_background".equals(entry.getKey()))
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        logger.debug("Configured {} parameters in Thymeleaf model", displayParameters.size());
        return displayParameters;
    }

    public Object resolveTemplateValue(Object value) {
        IEngineConfiguration configuration = templateEngine.getConfiguration();
        ExpressionContext expressionContext = new ExpressionContext(configuration);
        return toTemplateValue(value, configuration, expressionContext);
    }

    private Object toTemplateValue(
            Object value,
            IEngineConfiguration configuration,
            ExpressionContext expressionContext) {
        if (value instanceof String stringValue) {
            String trimmed = stringValue.trim();
            if (trimmed.startsWith("~{") && trimmed.endsWith("}")) {
                return value;
            }
        }
        return value;
    }

    /**
     * フラグメントレンダリング処理の結果データクラス
     * Infrastructure層のデータ転送専用
     */
    public static class FragmentRenderingContext {
        private final FragmentDiscoveryService.FragmentInfo selectedFragment;
        private final FragmentStoryInfo selectedStory;

        public FragmentRenderingContext(
                FragmentDiscoveryService.FragmentInfo selectedFragment,
                FragmentStoryInfo selectedStory) {
            this.selectedFragment = selectedFragment;
            this.selectedStory = selectedStory;
        }

        /**
         * レンダリングに必要な要素が揃っているかを検証
         * Infrastructure技術的制約チェック
         */
        public boolean isRenderingReady() {
            return selectedFragment != null && selectedStory != null;
        }
    }
}
