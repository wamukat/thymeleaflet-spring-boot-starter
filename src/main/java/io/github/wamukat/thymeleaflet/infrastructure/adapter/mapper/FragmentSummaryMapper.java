package io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper;

import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * FragmentSummary境界変換マッパー
 * 
 * Infrastructure層とDomain層の境界でのデータ変換を担当
 * Clean Architecture準拠:
 * - 境界での依存方向制御
 * - Infrastructure → Domain方向の変換
 * - Domain → Infrastructure方向の変換
 */
@Component
public class FragmentSummaryMapper {
    
    /**
     * Infrastructure FragmentInfo → Domain FragmentSummary変換
     */
    public FragmentSummary toDomain(FragmentDiscoveryService.FragmentInfo infrastructureFragmentInfo) {
        FragmentDiscoveryService.FragmentInfo fragmentInfo =
            Objects.requireNonNull(infrastructureFragmentInfo, "infrastructureFragmentInfo must not be null");
        
        return FragmentSummary.of(
            fragmentInfo.getTemplatePath(),
            fragmentInfo.getFragmentName(),
            fragmentInfo.getParameters(),
            fragmentInfo.getType()
        );
    }
    
    /**
     * Domain FragmentSummary → Infrastructure FragmentInfo変換
     */
    public FragmentDiscoveryService.FragmentInfo toInfrastructure(FragmentSummary domainFragmentSummary) {
        FragmentSummary fragmentSummary =
            Objects.requireNonNull(domainFragmentSummary, "domainFragmentSummary must not be null");
        
        return new FragmentDiscoveryService.FragmentInfo(
            fragmentSummary.getTemplatePath(),
            fragmentSummary.getFragmentName(),
            fragmentSummary.getParameters(),
            fragmentSummary.getType(),
            "" // originalDefinition は空文字列として設定
        );
    }
}
