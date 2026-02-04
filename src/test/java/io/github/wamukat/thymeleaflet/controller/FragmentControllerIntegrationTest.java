package io.github.wamukat.thymeleaflet.controller;

import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.MetricsUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.ValidationUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentDiscoveryUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentHierarchyUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentStatisticsUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.preview.FragmentPreviewUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryValidationUseCase;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.web.controller.FragmentListController;
import io.github.wamukat.thymeleaflet.infrastructure.web.controller.StoryPreviewController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * 分割後のFragmentコントローラー統合テスト
 * 基本的な動作確認のみ実施
 */
@SpringBootTest(classes = {io.github.wamukat.thymeleaflet.config.FragmentTestConfiguration.class})
@ActiveProfiles("test")
class FragmentControllerIntegrationTest {

    @MockBean
    private FragmentDiscoveryUseCase fragmentDiscoveryUseCase;
    
    @MockBean
    private FragmentStatisticsUseCase fragmentStatisticsUseCase;
    
    @MockBean
    private FragmentHierarchyUseCase fragmentHierarchyUseCase;
    
    @MockBean
    private FragmentPreviewUseCase fragmentPreviewUseCase;
    
    @MockBean
    private StoryRetrievalUseCase storyRetrievalUseCase;
    
    @MockBean
    private StoryValidationUseCase storyValidationUseCase;
    
    @MockBean
    private StoryParameterUseCase storyParameterUseCase;
    
    @MockBean
    private ValidationUseCase validationUseCase;
    
    @MockBean
    private MetricsUseCase metricsUseCase;
    
    @MockBean
    private FragmentDiscoveryService fragmentDiscoveryService;
    

    @Autowired
    private FragmentListController fragmentListController;
    
    @Autowired
    private StoryPreviewController storyPreviewController;

    @Test
    void fragmentListController_shouldReturnFragmentListView() {
        // Given
        Model model = new ExtendedModelMap();
        
        // Mock設定 - 最低1つのFragmentInfoを返すようにする
        FragmentDiscoveryService.FragmentInfo mockFragment = new FragmentDiscoveryService.FragmentInfo(
            "test/template", "testFragment", Collections.emptyList(), 
            FragmentDomainService.FragmentType.SIMPLE, "testFragment"
        );
        when(fragmentDiscoveryService.discoverFragments()).thenReturn(List.of(mockFragment));
        
        // When
        String viewName = fragmentListController.fragmentList(model);
        
        // Then
        assertEquals("thymeleaflet/fragment-list", viewName);
        assertNotNull(model.asMap().get("fragments"));
        assertNotNull(model.asMap().get("fragmentsJson"));
        assertNotNull(model.asMap().get("hierarchicalFragments"));
    }

    @Test
    void storyPreviewController_shouldReturnFragmentListView() {
        // Given
        Model model = new ExtendedModelMap();
        String templatePath = "test.template";
        String fragmentName = "testFragment";
        String storyName = "testStory";
        
        // SecureTemplatePath は直接作成可能なので、モック設定は不要
        
        // When
        String viewName = storyPreviewController.storyPreview(templatePath, fragmentName, storyName, model);
        
        // Then
        assertEquals("thymeleaflet/fragment-list", viewName);
        assertNotNull(model.asMap().get("templatePathEncoded"));
        assertNotNull(model.asMap().get("fragmentName"));
        assertNotNull(model.asMap().get("storyName"));
    }
}