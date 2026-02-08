package io.github.wamukat.thymeleaflet.infrastructure.web.controller;

import io.github.wamukat.thymeleaflet.infrastructure.web.service.FragmentRenderingService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * フラグメント動的レンダリング専用コントローラー
 * 
 * 責務: フラグメント動的レンダリングのHTTPエンドポイント
 */
@Controller
public class FragmentRenderingController {
    
    private static final Logger logger = LoggerFactory.getLogger(FragmentRenderingController.class);
    
    @Autowired
    private FragmentRenderingService fragmentRenderingService;
    
    
    /**
     * ストーリー動的プレビュー (HTMX用)
     * パス変数にスラッシュを含める場合は.*を使用してワイルドカードマッチング
     */
    @GetMapping("${thymeleaflet.base-path:/thymeleaflet}/{templatePath:.*}/{fragmentName}/{storyName}/render")
    public String renderStory(
            @PathVariable("templatePath") String templatePath,
            @PathVariable("fragmentName") String fragmentName,
            @PathVariable("storyName") String storyName,
            Model model) {
        
        FragmentRenderingService.RenderingResult result = 
            fragmentRenderingService.renderStory(templatePath, fragmentName, storyName, model);
        
        return result.templateReference()
            .orElse("thymeleaflet/fragments/error-display :: error(type='danger')");
    }

    /**
     * ストーリー動的プレビュー (POST: custom overrides)
     */
    @PostMapping("${thymeleaflet.base-path:/thymeleaflet}/{templatePath:.*}/{fragmentName}/{storyName}/render")
    public String renderStoryWithOverrides(
            @PathVariable("templatePath") String templatePath,
            @PathVariable("fragmentName") String fragmentName,
            @PathVariable("storyName") String storyName,
            @RequestBody(required = false) @Nullable RenderOverridesRequest request,
            Model model) {
        Map<String, Object> parameters = request != null ? request.parameters() : null;
        Map<String, Object> modelOverrides = request != null ? request.model() : null;
        FragmentRenderingService.RenderingResult result =
            fragmentRenderingService.renderStory(templatePath, fragmentName, storyName, model, parameters, modelOverrides);
        return result.templateReference()
            .orElse("thymeleaflet/fragments/error-display :: error(type='danger')");
    }

    public record RenderOverridesRequest(Map<String, Object> parameters, Map<String, Object> model) {}
}
