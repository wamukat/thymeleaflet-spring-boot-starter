package io.github.wamukat.thymeleaflet.infrastructure.web.controller;

import io.github.wamukat.thymeleaflet.infrastructure.web.service.UsageExampleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * フラグメントAPI専用コントローラー
 * 
 * 責務: AJAX・HTMX用のAPIエンドポイント（使用例生成など）
 */
@Controller
public class FragmentApiController {
    
    private static final Logger logger = LoggerFactory.getLogger(FragmentApiController.class);
    
    @Autowired
    private UsageExampleService usageExampleService;
    
    
    /**
     * 使用例生成API - プレビューの実際の状態に基づいて使用例を生成
     * パス変数にスラッシュを含める場合は.*を使用してワイルドカードマッチング
     */
    @GetMapping("${thymeleaflet.base-path:/thymeleaflet}/{templatePath:.*}/{fragmentName}/{storyName}/usage")
    public String getUsageExample(
            @PathVariable("templatePath") String templatePath,
            @PathVariable("fragmentName") String fragmentName,
            @PathVariable("storyName") String storyName,
            Model model) {
        
        UsageExampleService.UsageExampleResult result = 
            usageExampleService.generateUsageExample(templatePath, fragmentName, storyName, model);
        
        if (!result.succeeded()) {
            logger.error("Usage example generation failed: {}", result.errorMessage().orElse("unknown"));
            model.addAttribute("error", result.errorMessage().orElse("Usage example generation failed"));
            return "thymeleaflet/fragments/error-display :: error(type='danger')";
        }
        
        return "thymeleaflet/fragments/usage-example :: renderUsage";
    }
}
