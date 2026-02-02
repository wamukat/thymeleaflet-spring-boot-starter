package io.github.wamukat.thymeleaflet.application.service.fragment;

import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.MetricsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * フラグメントメトリクス記録サービス
 * 
 * 責務: メトリクス記録のみ
 * SRP準拠: 単一責任原則に従い、メトリクス記録のみを担当
 */
@Component
@Transactional(readOnly = true)
public class FragmentMetricsService implements MetricsUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(FragmentMetricsService.class);

    @Override
    public void logDiscoveryMetrics(MetricsCommand command) {
        logger.info("Discovery metrics - Time: {}ms, Fragments: {}", 
                   command.getDiscoveryTime(), 
                   command.getFragmentCount());
        
        // 発見処理メトリクスの詳細ログ出力
        if (command.getDiscoveryTime() > 1000) {
            logger.warn("Discovery process took longer than expected: {}ms", command.getDiscoveryTime());
        }
        
        if (command.getFragmentCount() == 0) {
            logger.warn("No fragments discovered");
        } else {
            logger.info("Successfully discovered {} fragments", command.getFragmentCount());
        }
    }
}