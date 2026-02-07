package io.github.wamukat.thymeleaflet.application.service.preview;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wamukat.thymeleaflet.application.port.inbound.preview.FragmentPreviewUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.coordination.StoryContentCoordinationUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import io.github.wamukat.thymeleaflet.infrastructure.web.service.JavaDocLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * フラグメントプレビュー専用ユースケース実装（縮小版）
 * 
 * 責務: フラグメントプレビューのみ
 * SRP準拠: 単一責任原則に従い、フラグメントプレビューのみを担当
 */
@Component
@Transactional(readOnly = true)
public class FragmentPreviewUseCaseImpl implements FragmentPreviewUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(FragmentPreviewUseCaseImpl.class);
    
    @Autowired
    private StoryContentCoordinationUseCase storyContentCoordinationUseCase;
    
    @Autowired
    private JavaDocLookupService javaDocLookupService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private io.github.wamukat.thymeleaflet.infrastructure.web.service.FragmentDependencyService fragmentDependencyService;

    @Override
    public PageSetupResponse setupStoryContentData(PageSetupCommand command) {
        StoryContentCoordinationUseCase.StoryContentRequest request =
            new StoryContentCoordinationUseCase.StoryContentRequest(
                command.getFullTemplatePath(),
                command.getFragmentName(),
                command.getStoryName(),
                command.getModel()
            );
        StoryContentCoordinationUseCase.StoryContentResult result =
            storyContentCoordinationUseCase.coordinateStoryContentSetup(request);

        if (!result.succeeded()) {
            logger.warn("setupStoryContentData: {}", result.errorMessage());
            return PageSetupResponse.failure(Objects.requireNonNullElse(result.errorMessage(), "Story setup failed"));
        }

        FragmentStoryInfo storyInfo = Objects.requireNonNull(result.storyInfo());
        Map<String, Object> displayModel = storyInfo.getModel();

        command.getModel().addAttribute("selectedFragment", result.selectedFragment());
        command.getModel().addAttribute("selectedStory", storyInfo);
        command.getModel().addAttribute("storyInfo", storyInfo);
        command.getModel().addAttribute("stories", result.stories());
        command.getModel().addAttribute("displayParameters", result.displayParameters());
        command.getModel().addAttribute("displayModel", displayModel);
        command.getModel().addAttribute("templatePathEncoded", command.getFullTemplatePath().replace("/", "."));
        command.getModel().addAttribute("javadocInfo", result.javadocInfo());
        command.getModel().addAttribute("dependentComponents",
            fragmentDependencyService.findDependencies(command.getFullTemplatePath(), command.getFragmentName()));
        command.getModel().addAttribute("defaultStory", result.defaultStory());
        command.getModel().addAttribute("defaultParameters", result.defaultParameters());

        logger.info("setupStoryContentData completed for {}::{}::{}", command.getFullTemplatePath(), command.getFragmentName(), command.getStoryName());

        return PageSetupResponse.success();
    }

    @Override
    public void setupFragmentJsonAttributes(List<Map<String, Object>> enrichedFragments, List<Map<String, Object>> hierarchicalFragments, Model model) {
        try {
            String enrichedJson = objectMapper.writeValueAsString(enrichedFragments);
            String hierarchicalJson = objectMapper.writeValueAsString(
                hierarchicalFragments != null && !hierarchicalFragments.isEmpty() ? hierarchicalFragments.get(0) : Map.of()
            );
            
            model.addAttribute("fragmentsJson", enrichedJson);
            model.addAttribute("hierarchicalJson", hierarchicalJson);
                
        } catch (Exception e) {
            logger.error("フラグメントデータのJSON変換に失敗しました", e);
            model.addAttribute("fragmentsJson", "[]");
            model.addAttribute("hierarchicalJson", "{}");
        }
    }

    @Override
    public JavaDocInfoResponse getJavaDocInfoWithDetailedLogging(JavaDocInfoCommand command) {
        return new JavaDocInfoResponse(javaDocLookupService.hasJavaDoc(command.templatePath(), command.fragmentName()));
    }

    @Override
    public @Nullable JavaDocAnalyzer.JavaDocInfo getJavaDocInfo(String templatePath, String fragmentName) {
        return javaDocLookupService.findJavaDocInfo(templatePath, fragmentName);
    }
}
