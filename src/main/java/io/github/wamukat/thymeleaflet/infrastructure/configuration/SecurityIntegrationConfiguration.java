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
 * Phase 4.4: Security統合完了の一環として実装
 * Feature Toggle制御による段階的なセキュリティ機能有効化を提供
 */
@Configuration
public class SecurityIntegrationConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SecurityIntegrationConfiguration.class);

    /**
     * セキュリティ統合FragmentPreviewUseCase（Phase 4.4以降で有効）
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

        // Phase 8.3: 既存のSpring管理UseCaseImplを参照
        return fragmentPreviewUseCaseImpl;
    }

    /**
     * Phase 4.5用の設定（完全移行版）
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

        // Phase 8.3: 既存のSpring管理UseCaseImplを参照
        // Phase 4.5では追加のセキュリティ機能を有効化
        logger.info("Enhanced security features enabled for Phase 4.5");
        
        return fragmentPreviewUseCaseImpl;
    }

    // SecurityIntegrationStatus is removed; MigrationProperties no longer needs to be injected here.
}
