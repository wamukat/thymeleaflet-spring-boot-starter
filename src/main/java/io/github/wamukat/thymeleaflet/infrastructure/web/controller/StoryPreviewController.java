package io.github.wamukat.thymeleaflet.infrastructure.web.controller;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.infrastructure.web.service.SecurePathConversionService;
import io.github.wamukat.thymeleaflet.infrastructure.web.service.StoryPreviewService;
import io.github.wamukat.thymeleaflet.infrastructure.web.service.StoryContentService;
import io.github.wamukat.thymeleaflet.infrastructure.web.service.ThymeleafletVersionResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * ストーリープレビュー表示専用コントローラー
 * 
 * 責務: ストーリープレビューページのHTTPエンドポイント
 */
@Controller
public class StoryPreviewController {
    
    @Autowired
    private StoryPreviewService storyPreviewService;
    
    @Autowired
    private StoryContentService storyContentService;

    @Autowired
    private StoryRetrievalUseCase storyRetrievalUseCase;

    @Autowired
    private SecurePathConversionService securePathConversionService;

    @Autowired
    private ThymeleafletVersionResolver thymeleafletVersionResolver;

    @Value("${thymeleaflet.base-path:/thymeleaflet}")
    private String basePath = "/thymeleaflet";
    
    /**
     * 個別ストーリープレビューページ（統一テンプレート構造を使用）
     * パス変数にスラッシュを含める場合は.*を使用してワイルドカードマッチング
     */
    @GetMapping("${thymeleaflet.base-path:/thymeleaflet}/{templatePath:.*}/{fragmentName}/{storyName}")
    public String storyPreview(
            @PathVariable("templatePath") String templatePath,
            @PathVariable("fragmentName") String fragmentName,
            @PathVariable("storyName") String storyName,
            Model model) {
        model.addAttribute("thymeleafletVersion", thymeleafletVersionResolver.resolve());
        Optional<String> redirectTarget = resolveRedirectTarget(templatePath, fragmentName, storyName, model);
        if (redirectTarget.isPresent()) {
            return redirectTarget.get();
        }

        StoryPreviewService.StoryPreviewResult result = 
            storyPreviewService.processStoryPreview(templatePath, fragmentName, storyName, model);
        
        return result.templateReference();
    }
    
    /**
     * HTMX用メインコンテンツエリア部分更新
     * パス変数にスラッシュを含める場合は.*を使用してワイルドカードマッチング
     */
    @GetMapping("${thymeleaflet.base-path:/thymeleaflet}/{templatePath:.*}/{fragmentName}/{storyName}/content")
    public String storyContentFragment(
            @PathVariable("templatePath") String templatePath,
            @PathVariable("fragmentName") String fragmentName,
            @PathVariable("storyName") String storyName,
            Model model) {
        model.addAttribute("thymeleafletVersion", thymeleafletVersionResolver.resolve());
        Optional<String> redirectTarget = resolveRedirectTarget(templatePath, fragmentName, storyName, model);
        if (redirectTarget.isPresent()) {
            return redirectTarget.get() + "/content";
        }

        StoryContentService.StoryContentResult result = 
            storyContentService.processStoryContent(templatePath, fragmentName, storyName, model);
        
        return result.templateReference();
    }
    
    private Optional<String> resolveRedirectTarget(String templatePath, String fragmentName, String storyName, Model model) {
        if (!"default".equals(storyName)) {
            return Optional.empty();
        }

        SecurePathConversionService.SecurityConversionResult conversionResult =
            securePathConversionService.convertSecurePath(templatePath, model);
        if (!conversionResult.succeeded()) {
            return Optional.empty();
        }
        String fullTemplatePath = conversionResult.fullTemplatePath().orElseThrow();

        StoryRetrievalUseCase.StoryListResponse listResponse =
            storyRetrievalUseCase.getStoriesForFragment(fullTemplatePath, fragmentName);
        if (!listResponse.isSuccess() || listResponse.getStories().isEmpty()) {
            return Optional.empty();
        }

        List<FragmentStoryInfo> stories = listResponse.getStories();
        boolean hasDefault = stories.stream().anyMatch(story -> "default".equals(story.getStoryName()));
        if (hasDefault) {
            return Optional.empty();
        }

        String firstStoryName = stories.get(0).getStoryName();
        if (firstStoryName.isBlank()) {
            return Optional.empty();
        }

        String encodedStory = UriUtils.encodePathSegment(firstStoryName, StandardCharsets.UTF_8);
        return Optional.of("redirect:" + basePath + "/" + templatePath + "/" + fragmentName + "/" + encodedStory);
    }
}
