package io.github.wamukat.thymeleaflet.application.service.fragment;

import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentStatisticsUseCase;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * フラグメント統計生成専用ユースケース実装
 * 
 * 責務: フラグメント統計生成のみ
 * SRP準拠: 単一責任原則に従い、フラグメント統計生成のみを担当
 */
@Component
@Transactional(readOnly = true)
public class FragmentStatisticsUseCaseImpl implements FragmentStatisticsUseCase {

    @Override
    public FragmentStatisticsResponse generateStatistics(List<FragmentDiscoveryService.FragmentInfo> fragments) {
        Map<String, Long> templateStats = fragments.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                fragment -> fragment.getTemplatePath().split("/")[0],
                java.util.stream.Collectors.counting()
            ));
            
        List<String> uniquePaths = fragments.stream()
            .map(FragmentDiscoveryService.FragmentInfo::getTemplatePath)
            .distinct()
            .sorted()
            .collect(java.util.stream.Collectors.toList());
        
        return FragmentStatisticsResponse.success(templateStats, uniquePaths);
    }
}