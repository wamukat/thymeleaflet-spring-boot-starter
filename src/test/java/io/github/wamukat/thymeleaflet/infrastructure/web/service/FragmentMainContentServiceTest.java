package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentHierarchyUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentStatisticsUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FragmentMainContentServiceTest {

    @Mock
    private FragmentDiscoveryService fragmentDiscoveryService;

    @Mock
    private FragmentStatisticsUseCase fragmentStatisticsUseCase;

    @Mock
    private FragmentHierarchyUseCase fragmentHierarchyUseCase;

    @Mock
    private FragmentJsonService fragmentJsonService;

    @Mock
    private FragmentSummaryMapper fragmentSummaryMapper;

    @Test
    void setupMainContent_setsPreviewResourcesFromProperties() {
        FragmentMainContentService service = new FragmentMainContentService();

        StorybookProperties properties = new StorybookProperties();
        StorybookProperties.ResourceConfig resources = new StorybookProperties.ResourceConfig();
        resources.setStylesheets(List.of("/css/app.css"));
        resources.setScripts(List.of("/js/app.js"));
        properties.setResources(resources);
        PreviewConfigService previewConfigService = buildPreviewConfigService(properties);

        ReflectionTestUtils.setField(service, "storybookConfig", ResolvedStorybookConfig.from(properties));
        ReflectionTestUtils.setField(service, "fragmentDiscoveryService", fragmentDiscoveryService);
        ReflectionTestUtils.setField(service, "fragmentStatisticsUseCase", fragmentStatisticsUseCase);
        ReflectionTestUtils.setField(service, "fragmentHierarchyUseCase", fragmentHierarchyUseCase);
        ReflectionTestUtils.setField(service, "fragmentJsonService", fragmentJsonService);
        ReflectionTestUtils.setField(service, "fragmentSummaryMapper", fragmentSummaryMapper);
        ReflectionTestUtils.setField(service, "previewConfigService", previewConfigService);

        FragmentDiscoveryService.FragmentInfo infraFragment = new FragmentDiscoveryService.FragmentInfo(
            "components/button",
            "primaryButton",
            List.of("label"),
            FragmentDomainService.FragmentType.PARAMETERIZED,
            "primaryButton(label)"
        );
        FragmentSummary summary = FragmentSummary.of(
            "components/button",
            "primaryButton",
            List.of("label"),
            FragmentDomainService.FragmentType.PARAMETERIZED
        );

        when(fragmentDiscoveryService.discoverFragments()).thenReturn(List.of(infraFragment));
        when(fragmentSummaryMapper.toDomain(infraFragment)).thenReturn(summary);
        when(fragmentStatisticsUseCase.generateStatistics(List.of(summary)))
            .thenReturn(FragmentStatisticsUseCase.FragmentStatisticsResponse.success(
                Map.of("components/button", 1L),
                List.of("components/button")
            ));
        when(fragmentHierarchyUseCase.buildHierarchicalStructure(List.of(summary)))
            .thenReturn(FragmentHierarchyUseCase.FragmentHierarchyResponse.success(
                Map.of("components", Map.of()), 1
            ));

        Model model = new ExtendedModelMap();

        FragmentMainContentService.MainContentResult result = service.setupMainContent(model);

        assertThat(result.succeeded()).isTrue();
        assertThat(model.getAttribute("previewStylesheets")).isEqualTo("/css/app.css");
        assertThat(model.getAttribute("previewScripts")).isEqualTo("/js/app.js");
    }

    private PreviewConfigService buildPreviewConfigService(StorybookProperties properties) {
        PreviewConfigService previewConfigService = new PreviewConfigService();
        StaticMessageSource messageSource = new StaticMessageSource();
        ReflectionTestUtils.setField(previewConfigService, "storybookConfig", ResolvedStorybookConfig.from(properties));
        ReflectionTestUtils.setField(previewConfigService, "messageSource", messageSource);
        return previewConfigService;
    }
}
