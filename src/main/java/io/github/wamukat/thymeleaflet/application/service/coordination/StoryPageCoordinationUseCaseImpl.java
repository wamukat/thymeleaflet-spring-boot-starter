package io.github.wamukat.thymeleaflet.application.service.coordination;

import io.github.wamukat.thymeleaflet.application.port.inbound.coordination.StoryPageCoordinationUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentDiscoveryUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentHierarchyUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentStatisticsUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.MetricsUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.preview.FragmentPreviewUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryRetrievalUseCase;
import io.github.wamukat.thymeleaflet.application.port.outbound.FragmentCatalogPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ストーリーページ協調ユースケース実装
 * 
 * 複数UseCaseの協調処理をApplication層で実施
 * StoryPreviewController肥大化問題の解決を目的とする
 */
@Component
@Transactional(readOnly = true)
public class StoryPageCoordinationUseCaseImpl implements StoryPageCoordinationUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(StoryPageCoordinationUseCaseImpl.class);
    
    @Autowired
    private FragmentDiscoveryUseCase fragmentDiscoveryUseCase;
    
    @Autowired
    private FragmentStatisticsUseCase fragmentStatisticsUseCase;
    
    @Autowired
    private FragmentHierarchyUseCase fragmentHierarchyUseCase;
    
    @Autowired
    private MetricsUseCase metricsUseCase;
    
    @Autowired
    private StoryRetrievalUseCase storyRetrievalUseCase;
    
    @Autowired
    private FragmentPreviewUseCase fragmentPreviewUseCase;
    
    @Autowired
    private FragmentCatalogPort fragmentCatalogPort;
    
    @Override
    public StoryPageResult coordinateStoryPageSetup(StoryPageRequest request) {
        logger.info("=== StoryPageCoordination START ===");
        logger.info("Coordinating page setup for: {}::{}::{}", 
                   request.fullTemplatePath(), request.fragmentName(), request.storyName());
        
        try {
            // 1. フラグメント発見・統計・階層化の協調処理
            long discoveryStart = System.currentTimeMillis();
            List<FragmentSummary> allFragments = fragmentCatalogPort.discoverFragments();
            
            // メトリクス記録
            long discoveryTime = System.currentTimeMillis() - discoveryStart;
            MetricsUseCase.MetricsCommand metricsCommand = 
                new MetricsUseCase.MetricsCommand(discoveryTime, allFragments.size());
            metricsUseCase.logDiscoveryMetrics(metricsCommand);
            
            // フラグメントタイプ別グループ化 (Domain形式で実行)
            Map<FragmentDomainService.FragmentType, List<FragmentSummary>> groupedFragments = 
                allFragments.stream()
                    .collect(Collectors.groupingBy(FragmentSummary::getType));
            
            // 統計情報生成
            FragmentStatisticsUseCase.FragmentStatisticsResponse statisticsResponse = 
                fragmentStatisticsUseCase.generateStatistics(allFragments);
            Map<String, Long> templateStats = statisticsResponse.getTemplateStats();
            List<String> uniquePaths = statisticsResponse.getUniquePaths();
            
            // 階層構造化
            FragmentHierarchyUseCase.FragmentHierarchyResponse hierarchyResponse = 
                fragmentHierarchyUseCase.buildHierarchicalStructure(allFragments);
            Map<String, Object> hierarchicalFragments = hierarchyResponse.getHierarchicalStructure();
            
            // 2. 選択されたフラグメント・ストーリー取得
            FragmentDiscoveryUseCase.FragmentDetailResponse fragmentDetailResponse = 
                fragmentDiscoveryUseCase.discoverFragment(request.fullTemplatePath(), request.fragmentName());
            Optional<FragmentSummary> selectedFragment = fragmentDetailResponse.getFragment();

            Optional<FragmentStoryInfo> selectedStory = Optional.empty();
            if (selectedFragment.isPresent()) {
                selectedStory = storyRetrievalUseCase.getStory(
                    request.fullTemplatePath(),
                    request.fragmentName(),
                    request.storyName()
                );
            }
            
            // 3. Modelに統合結果を設定 (Domain形式のFragmentSummaryを設定)
            request.model().addAttribute("allFragments", allFragments); // Domain形式のFragmentSummary
            request.model().addAttribute("groupedFragments", groupedFragments);
            request.model().addAttribute("templateStats", templateStats);
            request.model().addAttribute("uniquePaths", uniquePaths);
            request.model().addAttribute("hierarchicalFragments", hierarchicalFragments);
            request.model().addAttribute("totalCount", allFragments.size());
            request.model().addAttribute("selectedFragment", selectedFragment.orElse(null));
            request.model().addAttribute("selectedStory", selectedStory.orElse(null));
            request.model().addAttribute("storyInfo", selectedStory.orElse(null));
            
            // 4. ストーリーコンテンツデータ設定 (旧setupStoryContentData相当処理)
            if (selectedFragment.isPresent() && selectedStory.isPresent()) {
                FragmentPreviewUseCase.PageSetupCommand pageSetupCommand = 
                    new FragmentPreviewUseCase.PageSetupCommand(request.fullTemplatePath(), request.fragmentName(), request.storyName(), request.model());
                FragmentPreviewUseCase.PageSetupResponse pageSetupResponse = fragmentPreviewUseCase.setupStoryContentData(pageSetupCommand);
                
                if (!pageSetupResponse.isSucceeded()) {
                    logger.warn("Story content data setup failed: {}, but continuing", pageSetupResponse.errorMessage().orElse("unknown"));
                }
            }
            
            // 5. 選択状態を設定（ナビゲーション状態保持のために重要）
            request.model().addAttribute("selectedTemplatePath", request.fullTemplatePath());
            request.model().addAttribute("selectedFragmentName", request.fragmentName());
            request.model().addAttribute("selectedStoryName", request.storyName());
            // URL用のSecureTemplatePathを生成（元のパス形式から）
            String originalTemplatePath = request.fullTemplatePath().replace("/", ".");
            io.github.wamukat.thymeleaflet.domain.model.SecureTemplatePath secureTemplatePathForMain = 
                io.github.wamukat.thymeleaflet.domain.model.SecureTemplatePath.createUnsafe(originalTemplatePath);
            request.model().addAttribute("templatePathEncoded", secureTemplatePathForMain.forUrl());
            
            logger.info("=== StoryPageCoordination COMPLETED ===");
            return StoryPageResult.success();
            
        } catch (Exception e) {
            logger.error("Story page coordination failed", e);
            return StoryPageResult.failure("ページセットアップに失敗しました: " + e.getMessage());
        }
    }
}
