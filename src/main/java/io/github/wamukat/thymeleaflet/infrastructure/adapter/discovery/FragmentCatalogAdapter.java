package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import io.github.wamukat.thymeleaflet.application.port.outbound.FragmentCatalogPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FragmentCatalogPort の infrastructure 実装。
 */
@Component
public class FragmentCatalogAdapter implements FragmentCatalogPort {

    private final FragmentDiscoveryService fragmentDiscoveryService;
    private final FragmentSummaryMapper fragmentSummaryMapper;

    public FragmentCatalogAdapter(
        FragmentDiscoveryService fragmentDiscoveryService,
        FragmentSummaryMapper fragmentSummaryMapper
    ) {
        this.fragmentDiscoveryService = fragmentDiscoveryService;
        this.fragmentSummaryMapper = fragmentSummaryMapper;
    }

    @Override
    public List<FragmentSummary> discoverFragments() {
        return fragmentDiscoveryService.discoverFragments().stream()
            .map(fragmentSummaryMapper::toDomain)
            .toList();
    }
}

