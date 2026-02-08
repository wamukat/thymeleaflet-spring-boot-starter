package io.github.wamukat.thymeleaflet.application.service.preview;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wamukat.thymeleaflet.application.port.inbound.preview.FragmentPreviewUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.coordination.StoryContentCoordinationUseCase;
import io.github.wamukat.thymeleaflet.application.port.outbound.FragmentDependencyPort;
import io.github.wamukat.thymeleaflet.application.port.outbound.JavaDocLookupPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

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
    private JavaDocLookupPort javaDocLookupPort;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FragmentDependencyPort fragmentDependencyPort;

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
            logger.warn("setupStoryContentData: {}", result.errorMessage().orElse("Story setup failed"));
            return PageSetupResponse.failure(result.errorMessage().orElse("Story setup failed"));
        }

        FragmentStoryInfo storyInfo = result.storyInfo().orElseThrow();
        Map<String, Object> displayModel = storyInfo.getModel();

        command.getModel().addAttribute("selectedFragment", result.selectedFragment().orElse(null));
        command.getModel().addAttribute("selectedStory", storyInfo);
        command.getModel().addAttribute("storyInfo", storyInfo);
        command.getModel().addAttribute("stories", result.stories().orElse(List.of()));
        command.getModel().addAttribute("displayParameters", result.displayParameters().orElse(Map.of()));
        command.getModel().addAttribute("displayModel", displayModel);
        command.getModel().addAttribute("templatePathEncoded", command.getFullTemplatePath().replace("/", "."));
        command.getModel().addAttribute("javadocInfo", result.javadocInfo().orElse(null));
        command.getModel().addAttribute("dependentComponents",
            fragmentDependencyPort.findDependenciesForView(command.getFullTemplatePath(), command.getFragmentName()));
        command.getModel().addAttribute("defaultStory", result.defaultStory().orElse(null));
        command.getModel().addAttribute("defaultParameters", result.defaultParameters().orElse(Map.of()));

        logger.info("setupStoryContentData completed for {}::{}::{}", command.getFullTemplatePath(), command.getFragmentName(), command.getStoryName());

        return PageSetupResponse.success();
    }

    @Override
    public void setupFragmentJsonAttributes(List<Map<String, Object>> enrichedFragments, List<Map<String, Object>> hierarchicalFragments, Model model) {
        try {
            String enrichedJson = objectMapper.writeValueAsString(enrichedFragments);
            String hierarchicalJson = objectMapper.writeValueAsString(
                !hierarchicalFragments.isEmpty() ? hierarchicalFragments.get(0) : Map.of()
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
        return new JavaDocInfoResponse(javaDocLookupPort.hasJavaDoc(command.templatePath(), command.fragmentName()));
    }

}
