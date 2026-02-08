package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentHierarchyUseCase;
import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentStatisticsUseCase;
import io.github.wamukat.thymeleaflet.domain.service.FragmentDomainService;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.mapper.FragmentSummaryMapper;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * フラグメントメインコンテンツ処理専用サービス
 * 
 * 責務: フラグメント一覧ページのメインコンテンツ遅延読み込み処理
 * FragmentListController肥大化問題解決のためのInfrastructure層サービス抽出
 */
@Component
public class FragmentMainContentService {
    
    private static final Logger logger = LoggerFactory.getLogger(FragmentMainContentService.class);
    
    @Autowired
    private FragmentDiscoveryService fragmentDiscoveryService;
    
    @Autowired
    private FragmentStatisticsUseCase fragmentStatisticsUseCase;
    
    @Autowired
    private FragmentHierarchyUseCase fragmentHierarchyUseCase;
    
    @Autowired
    private FragmentJsonService fragmentJsonService;
    
    @Autowired
    private FragmentSummaryMapper fragmentSummaryMapper;

    @Autowired
    private ResolvedStorybookConfig storybookConfig;

    @Autowired
    private PreviewConfigService previewConfigService;
    
    /**
     * メインコンテンツ遅延読み込み処理
     * 
     * 完全なフラグメント情報の取得・処理・JSON変換をまとめて実行
     * 
     * @param model Spring MVCモデル
     * @return 処理結果レポート
     */
    public MainContentResult setupMainContent(Model model) {
        long startTime = System.currentTimeMillis();
        logger.info("=== Main Content (Delayed Loading) START ===");
        
        try {
            // 完全なフラグメント情報を取得・処理
            List<FragmentDiscoveryService.FragmentInfo> infraFragments = fragmentDiscoveryService.discoverFragments();
            
            // Infrastructure形式からDomain形式に変換
            List<FragmentSummary> allFragments = infraFragments.stream()
                .map(fragmentSummaryMapper::toDomain)
                .collect(Collectors.toList());
            
            // フラグメントタイプ別にグループ化
            Map<FragmentDomainService.FragmentType, List<FragmentSummary>> groupedFragments = 
                allFragments.stream()
                    .collect(Collectors.groupingBy(FragmentSummary::getType));
            
            // テンプレートパス別の統計
            FragmentStatisticsUseCase.FragmentStatisticsResponse statisticsResponse = 
                fragmentStatisticsUseCase.generateStatistics(allFragments);
            Map<String, Long> templateStats = statisticsResponse.getTemplateStats();
            
            // ユニークパスリスト
            List<String> uniquePaths = statisticsResponse.getUniquePaths();
            
            // 階層構造化 - ナビソート機能含む
            FragmentHierarchyUseCase.FragmentHierarchyResponse hierarchyResponse = 
                fragmentHierarchyUseCase.buildHierarchicalStructure(allFragments);
            Map<String, Object> hierarchicalFragments = hierarchyResponse.getHierarchicalStructure();
            
            // JSON変換とモデル属性設定 (Domain形式を使用)
            fragmentJsonService.setupFragmentJsonAttributes(allFragments, hierarchicalFragments, model);
            
            model.addAttribute("allFragments", allFragments); // Domain形式のFragmentSummary
            model.addAttribute("groupedFragments", groupedFragments);
            model.addAttribute("templateStats", templateStats);
            model.addAttribute("uniquePaths", uniquePaths);
            model.addAttribute("hierarchicalFragments", hierarchicalFragments);
            model.addAttribute("totalCount", allFragments.size());
            model.addAttribute("previewStylesheets", joinResources(storybookConfig.getResources().getStylesheets()));
            model.addAttribute("previewScripts", joinResources(storybookConfig.getResources().getScripts()));
            previewConfigService.applyPreviewConfig(model);
            
            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("=== Main Content (Delayed Loading) COMPLETED in {} ms ===", totalTime);
            
            return MainContentResult.success(allFragments.size(), totalTime);
            
        } catch (Exception e) {
            logger.error("Main content setup failed", e);
            return MainContentResult.failure("メインコンテンツの設定に失敗しました: " + e.getMessage());
        }
    }
    
    /**
     * メインコンテンツ処理結果
     */
    public static class MainContentResult {
        private final boolean succeeded;
        private final Optional<String> errorMessage;
        private final int fragmentCount;
        private final long processingTime;
        
        private MainContentResult(boolean succeeded, Optional<String> errorMessage, int fragmentCount, long processingTime) {
            this.succeeded = succeeded;
            this.errorMessage = errorMessage;
            this.fragmentCount = fragmentCount;
            this.processingTime = processingTime;
        }
        
        public static MainContentResult success(int fragmentCount, long processingTime) {
            return new MainContentResult(true, Optional.empty(), fragmentCount, processingTime);
        }
        
        public static MainContentResult failure(String errorMessage) {
            return new MainContentResult(false, Optional.of(errorMessage), 0, 0);
        }
        
        public boolean succeeded() { return succeeded; }
        public Optional<String> errorMessage() { return errorMessage; }
        public int fragmentCount() { return fragmentCount; }
        public long processingTime() { return processingTime; }
    }

    private String joinResources(List<String> resources) {
        if (resources.isEmpty()) {
            return "";
        }
        return resources.stream()
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.joining(","));
    }
}
