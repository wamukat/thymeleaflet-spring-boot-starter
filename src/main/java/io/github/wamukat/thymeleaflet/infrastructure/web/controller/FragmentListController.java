package io.github.wamukat.thymeleaflet.infrastructure.web.controller;

import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import io.github.wamukat.thymeleaflet.infrastructure.web.service.FragmentMainContentService;
import io.github.wamukat.thymeleaflet.infrastructure.web.service.ThymeleafletVersionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * フラグメント一覧表示専用コントローラー
 * 
 * 責務: フラグメント一覧ページの表示とメインコンテンツ遅延読み込み
 */
@Controller
public class FragmentListController {
    
    private static final Logger logger = LoggerFactory.getLogger(FragmentListController.class);

    @Autowired
    private FragmentDiscoveryService fragmentDiscoveryService;
    
    @Autowired
    private FragmentMainContentService fragmentMainContentService;

    @Autowired
    private ThymeleafletVersionResolver thymeleafletVersionResolver;
    
    /**
     * Storybookメインエントリーポイント - フラグメント一覧ページ (プレースホルダ最適化版)
     */
    @GetMapping({
            "${thymeleaflet.base-path:/thymeleaflet}",
            "${thymeleaflet.base-path:/thymeleaflet}/"
    })
    public String fragmentList(Model model) {
        long startTime = System.currentTimeMillis();
        logger.info("=== Fragment List (Placeholder Optimized) START ===");
        model.addAttribute("thymeleafletVersion", thymeleafletVersionResolver.resolve());
        
        // 初期レンダリング時は重い処理をスキップ - フラグメント発見のみ実行
        List<FragmentDiscoveryService.FragmentInfo> fragments = fragmentDiscoveryService.discoverFragments();
        int totalCount = fragments.size();
        logger.info("Fragment discovery using direct service: {} fragments", totalCount);
        
        // 基本的なフラグメント発見（エラーハンドリングは簡略化）
        if (fragments.isEmpty()) {
            logger.warn("No fragments found");
            model.addAttribute("error", "フラグメントが見つかりませんでした");
            return "thymeleaflet/fragment-list";
        }
        
        // 基本的な統計のみ計算（プレースホルダ表示用）
        int uniquePathsCount = (int) fragments.stream()
            .map(FragmentDiscoveryService.FragmentInfo::getTemplatePath)
            .distinct()
            .count();
        
        // 外部仕様として必要な属性を設定（契約テスト保護）
        model.addAttribute("fragments", fragments); // 契約テスト必須属性
        model.addAttribute("fragmentsJson", "[]"); // クライアントサイドでの初期化用
        model.addAttribute("hierarchicalFragments", new HashMap<>()); // 契約テスト必須属性
        model.addAttribute("hierarchicalFragmentsJson", "{}"); // 契約テスト必須属性（hierarchicalJsonの正式名）
        model.addAttribute("hierarchicalJson", "{}"); // 既存互換性保持
        model.addAttribute("totalCount", totalCount);
        
        // プレースホルダ用の簡易統計（実際のパスリスト）
        List<String> uniquePaths = fragments.stream()
            .map(FragmentDiscoveryService.FragmentInfo::getTemplatePath)
            .distinct()
            .collect(Collectors.toList());
        model.addAttribute("uniquePaths", uniquePaths);
        
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("=== Fragment List (Placeholder Optimized) COMPLETED in {} ms ===", totalTime);
        
        return "thymeleaflet/fragment-list";
    }
    
    /**
     * メインコンテンツ遅延読み込みエンドポイント
     */
    @GetMapping("${thymeleaflet.base-path:/thymeleaflet}/main-content")
    public String mainContent(Model model) {
        FragmentMainContentService.MainContentResult result = fragmentMainContentService.setupMainContent(model);
        
        if (!result.succeeded()) {
            logger.error("Main content setup failed: {}", result.errorMessage());
            model.addAttribute("error", result.errorMessage());
            return "thymeleaflet/fragments/error-display :: error(type='danger')";
        }
        
        // フラグメント部分テンプレートを返す
        return "thymeleaflet/fragments/main-content :: delayedContent";
    }

}
