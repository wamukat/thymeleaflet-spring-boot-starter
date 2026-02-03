package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import io.github.wamukat.thymeleaflet.application.port.inbound.preview.FragmentPreviewUseCase;
import io.github.wamukat.thymeleaflet.application.service.preview.FragmentPreviewUseCaseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * セキュリティ統合設定 - Configuration
 *
 * Migration phase に応じて FragmentPreviewUseCase を切り替える。
 */
@Configuration
public class SecurityIntegrationConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SecurityIntegrationConfiguration.class);

    /**
     * Migration phase 4.4 向け設定
     */
    @Bean
    @ConditionalOnProperty(
        name = "thymeleaflet.migration.phase", 
        havingValue = "4.4", 
        matchIfMissing = false
    )
    public FragmentPreviewUseCase securityIntegratedFragmentPreviewUseCase(
            FragmentPreviewUseCaseImpl fragmentPreviewUseCaseImpl) {
        
        logger.info("Configuring Security Integrated Fragment Preview UseCase - referencing existing bean");

        return fragmentPreviewUseCaseImpl;
    }

    /**
     * Migration phase 4.5 向け設定
     */
    @Bean
    @ConditionalOnProperty(
        name = "thymeleaflet.migration.phase", 
        havingValue = "4.5", 
        matchIfMissing = false
    )
    public FragmentPreviewUseCase fullyMigratedFragmentPreviewUseCase(
            FragmentPreviewUseCaseImpl fragmentPreviewUseCaseImpl) {
        
        logger.info("Configuring Fully Migrated Fragment Preview UseCase for migration phase: 4.5");
        
        return fragmentPreviewUseCaseImpl;
    }

    // SecurityIntegrationStatus is removed; MigrationProperties no longer needs to be injected here.
}
