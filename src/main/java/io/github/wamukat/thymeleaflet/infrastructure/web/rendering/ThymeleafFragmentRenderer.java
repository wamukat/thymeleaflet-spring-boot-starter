package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import io.github.wamukat.thymeleaflet.application.port.outbound.StoryPresentationPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ExpressionContext;
import org.springframework.stereotype.Component;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Thymeleafテンプレートレンダリング専用Infrastructure実装
 * <p>
 * Phase 5.1: FragmentPreviewServiceからの技術的責任抽出
 * Pure Infrastructure責任: テンプレートレンダリング技術的処理のみ
 */
@Component
public class ThymeleafFragmentRenderer implements StoryPresentationPort {

    private static final Logger logger = LoggerFactory.getLogger(ThymeleafFragmentRenderer.class);

    private final SpringTemplateEngine templateEngine;

    public ThymeleafFragmentRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * 指定されたテンプレートパスとフラグメント名に一致するフラグメントを検索
     * Infrastructure責任: フラグメント選択の技術的処理
     */
    public Optional<FragmentDiscoveryService.FragmentInfo> findFragmentByIdentifier(
            List<FragmentDiscoveryService.FragmentInfo> allFragments,
            String templatePath,
            String fragmentName) {

        long selectionStart = System.currentTimeMillis();

        Optional<FragmentDiscoveryService.FragmentInfo> selectedFragment = allFragments.stream()
                .filter(f -> f.getTemplatePath().equals(templatePath) &&
                             f.getFragmentName().equals(fragmentName))
                .findFirst();

        logger.debug("Fragment selection took {} ms, selected: {}",
                System.currentTimeMillis() - selectionStart,
                selectedFragment.map(FragmentDiscoveryService.FragmentInfo::getFragmentName).orElse("none"));

        return selectedFragment;
    }

    /**
     * ストーリーパラメータをThymeleafモデルに設定し、表示用パラメータを返す
     * Infrastructure責任: Thymeleaf固有のモデル操作
     */
    @Override
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
                .filter(entry -> Objects.nonNull(entry.getValue()))
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
        private final Optional<FragmentDiscoveryService.FragmentInfo> selectedFragment;
        private final Optional<FragmentStoryInfo> selectedStory;

        public FragmentRenderingContext(
                Optional<FragmentDiscoveryService.FragmentInfo> selectedFragment,
                Optional<FragmentStoryInfo> selectedStory) {
            this.selectedFragment = Objects.requireNonNull(selectedFragment, "selectedFragment cannot be null");
            this.selectedStory = Objects.requireNonNull(selectedStory, "selectedStory cannot be null");
        }

        /**
         * レンダリングに必要な要素が揃っているかを検証
         * Infrastructure技術的制約チェック
         */
        public boolean isRenderingReady() {
            return selectedFragment.isPresent() && selectedStory.isPresent();
        }
    }
}
