package io.github.wamukat.thymeleaflet.application.service.fragment;

import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentDiscoveryUseCase;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * フラグメント発見専用ユースケース実装
 * 
 * 責務: フラグメント発見のみ
 * SRP準拠: 単一責任原則に従い、フラグメント発見のみを担当
 */
@Component
@Transactional(readOnly = true)
public class FragmentDiscoveryUseCaseImpl implements FragmentDiscoveryUseCase {
    
    @Autowired
    private FragmentDiscoveryService fragmentDiscoveryService;

    @Autowired
    private FragmentSummaryMapper fragmentSummaryMapper;

    @Override
    public FragmentDetailResponse discoverFragment(String templatePath, String fragmentName) {
        List<FragmentDiscoveryService.FragmentInfo> allFragments = fragmentDiscoveryService.discoverFragments();

        return allFragments.stream()
            .filter(f -> f.getTemplatePath().equals(templatePath) && f.getFragmentName().equals(fragmentName))
            .findFirst()
            .map(fragment -> FragmentDetailResponse.success(fragmentSummaryMapper.toDomain(fragment), templatePath, fragmentName))
            .orElseGet(() -> FragmentDetailResponse.notFound(templatePath, fragmentName));
    }
}
