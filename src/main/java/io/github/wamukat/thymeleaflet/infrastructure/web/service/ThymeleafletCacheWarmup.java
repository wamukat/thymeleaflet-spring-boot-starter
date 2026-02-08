package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocContentService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.service.DocumentationAnalysisAdapter;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ThymeleafletCacheWarmup {

    private static final Logger logger = LoggerFactory.getLogger(ThymeleafletCacheWarmup.class);

    private final ResolvedStorybookConfig storybookConfig;
    private final FragmentDiscoveryService fragmentDiscoveryService;
    private final JavaDocContentService javaDocContentService;
    private final DocumentationAnalysisAdapter documentationAnalysisAdapter;
    private final FragmentDependencyService fragmentDependencyService;

    public ThymeleafletCacheWarmup(ResolvedStorybookConfig storybookConfig,
                                   FragmentDiscoveryService fragmentDiscoveryService,
                                   JavaDocContentService javaDocContentService,
                                   DocumentationAnalysisAdapter documentationAnalysisAdapter,
                                   FragmentDependencyService fragmentDependencyService) {
        this.storybookConfig = storybookConfig;
        this.fragmentDiscoveryService = fragmentDiscoveryService;
        this.javaDocContentService = javaDocContentService;
        this.documentationAnalysisAdapter = documentationAnalysisAdapter;
        this.fragmentDependencyService = fragmentDependencyService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void preloadCaches() {
        if (!storybookConfig.getCache().isEnabled() || !storybookConfig.getCache().isPreload()) {
            return;
        }

        logger.info("Starting Thymeleaflet cache warmup");
        List<FragmentDiscoveryService.FragmentInfo> fragments = fragmentDiscoveryService.discoverFragments();

        Set<String> templatePaths = new HashSet<>();
        for (FragmentDiscoveryService.FragmentInfo fragment : fragments) {
            templatePaths.add(fragment.getTemplatePath());
        }

        for (String templatePath : templatePaths) {
            javaDocContentService.loadJavaDocInfos(templatePath);
            documentationAnalysisAdapter.extractTypeInformation(templatePath);
        }

        for (FragmentDiscoveryService.FragmentInfo fragment : fragments) {
            fragmentDependencyService.findDependencies(fragment.getTemplatePath(), fragment.getFragmentName());
        }

        logger.info("Completed Thymeleaflet cache warmup (templates: {}, fragments: {})",
            templatePaths.size(), fragments.size());
    }
}
