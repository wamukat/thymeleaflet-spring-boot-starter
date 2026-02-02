package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ThymeleafFragmentRenderer用ユニットテスト
 * Phase 5.1: Infrastructure層テンプレートレンダリング機能のテスト
 */
@DisplayName("ThymeleafFragmentRenderer Tests")
class ThymeleafFragmentRendererTest {

    private ThymeleafFragmentRenderer renderer;

    @BeforeEach
    void setUp() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(new StringTemplateResolver());
        renderer = new ThymeleafFragmentRenderer(templateEngine);
    }

    @Test
    @DisplayName("フラグメント識別子による検索が正常に動作する")
    void shouldFindFragmentByIdentifier() {
        // Given
        List<FragmentDiscoveryService.FragmentInfo> allFragments = Arrays.asList(
            createFragmentInfo("templates/shared/atoms/button", "primary-button"),
            createFragmentInfo("templates/shared/atoms/button", "secondary-button"),
            createFragmentInfo("templates/domain/member/atoms/avatar", "member-avatar")
        );

        // When
        FragmentDiscoveryService.FragmentInfo found = renderer.findFragmentByIdentifier(
            allFragments, "templates/shared/atoms/button", "primary-button"
        );

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getTemplatePath()).isEqualTo("templates/shared/atoms/button");
        assertThat(found.getFragmentName()).isEqualTo("primary-button");
    }

    @Test
    @DisplayName("存在しないフラグメントの場合nullを返す")
    void shouldReturnNullForNonexistentFragment() {
        // Given
        List<FragmentDiscoveryService.FragmentInfo> allFragments = Arrays.asList(
            createFragmentInfo("templates/shared/atoms/button", "primary-button")
        );

        // When
        FragmentDiscoveryService.FragmentInfo found = renderer.findFragmentByIdentifier(
            allFragments, "templates/nonexistent", "unknown-fragment"
        );

        // Then
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("Thymeleafモデルにパラメータが正常に設定される")
    void shouldConfigureModelWithStoryParameters() {
        // Given
        Map<String, Object> storyParameters = new HashMap<>();
        storyParameters.put("text", "Test Button");
        storyParameters.put("variant", "primary");
        storyParameters.put("disabled", false);
        storyParameters.put("__storybook_background", "#ffffff"); // 除外されるべきパラメータ

        Model model = new ExtendedModelMap();

        // When
        Map<String, Object> displayParameters = renderer.configureModelWithStoryParameters(storyParameters, model);

        // Then - モデルに__storybook_background以外が設定されること
        assertThat(model.asMap()).hasSize(3);
        assertThat(model.asMap()).containsEntry("text", "Test Button");
        assertThat(model.asMap()).containsEntry("variant", "primary");
        assertThat(model.asMap()).containsEntry("disabled", false);
        assertThat(model.asMap()).doesNotContainKey("__storybook_background");

        // Then - 表示用パラメータも__storybook_background以外が含まれること
        assertThat(displayParameters).hasSize(3);
        assertThat(displayParameters).containsEntry("text", "Test Button");
        assertThat(displayParameters).containsEntry("variant", "primary");
        assertThat(displayParameters).containsEntry("disabled", false);
        assertThat(displayParameters).doesNotContainKey("__storybook_background");
    }

    @Test
    @DisplayName("空のパラメータマップも正常に処理される")
    void shouldHandleEmptyParameters() {
        // Given
        Map<String, Object> emptyParameters = new HashMap<>();
        Model model = new ExtendedModelMap();

        // When
        Map<String, Object> displayParameters = renderer.configureModelWithStoryParameters(emptyParameters, model);

        // Then
        assertThat(model.asMap()).isEmpty();
        assertThat(displayParameters).isEmpty();
    }

    @Test
    @DisplayName("FragmentRenderingContextのレンダリング準備状態判定が正常に動作する")
    void shouldCorrectlyDetermineRenderingReadiness() {
        // Given - レンダリング準備完了の場合
        ThymeleafFragmentRenderer.FragmentRenderingContext readyContext = 
            new ThymeleafFragmentRenderer.FragmentRenderingContext(
                createFragmentInfo("template", "fragment"),
                createFragmentStoryInfo("story")
            );

        // When & Then
        assertThat(readyContext.isRenderingReady()).isTrue();
        
        // Given - フラグメントが未選択の場合
        ThymeleafFragmentRenderer.FragmentRenderingContext notReadyContext = 
            new ThymeleafFragmentRenderer.FragmentRenderingContext(
                null, // フラグメント未選択
                createFragmentStoryInfo("story")
            );

        // When & Then
        assertThat(notReadyContext.isRenderingReady()).isFalse();
    }

    @Test
    @DisplayName("__storybook_backgroundのみのパラメータでも正常に処理される")
    void shouldHandleOnlyStorybookBackground() {
        // Given
        Map<String, Object> backgroundOnlyParams = new HashMap<>();
        backgroundOnlyParams.put("__storybook_background", "#f0f0f0");

        Model model = new ExtendedModelMap();

        // When
        Map<String, Object> displayParameters = renderer.configureModelWithStoryParameters(backgroundOnlyParams, model);

        // Then
        assertThat(model.asMap()).isEmpty();
        assertThat(displayParameters).isEmpty();
    }

    // テストヘルパーメソッド

    private FragmentDiscoveryService.FragmentInfo createFragmentInfo(String templatePath, String fragmentName) {
        return new FragmentDiscoveryService.FragmentInfo(
            templatePath, 
            fragmentName, 
            Arrays.asList("param1", "param2"), 
            FragmentDomainService.FragmentType.PARAMETERIZED,
            "th:fragment=\"" + fragmentName + "\""
        );
    }

    private io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo createFragmentStoryInfo(String storyName) {
        // FragmentStoryInfoのモックオブジェクトを作成（簡易実装）
        return io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo.of(
            createDomainFragmentSummary("template", "fragment"), 
            "fragment", 
            storyName, 
            null
        );
    }
    
    private io.github.wamukat.thymeleaflet.domain.model.FragmentSummary createDomainFragmentSummary(String templatePath, String fragmentName) {
        return io.github.wamukat.thymeleaflet.domain.model.FragmentSummary.of(
            templatePath, 
            fragmentName, 
            Arrays.asList("param1", "param2"), 
            FragmentDomainService.FragmentType.PARAMETERIZED
        );
    }
}
