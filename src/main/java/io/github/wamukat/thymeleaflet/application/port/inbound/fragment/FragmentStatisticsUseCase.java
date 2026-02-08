package io.github.wamukat.thymeleaflet.application.port.inbound.fragment;

import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;

import java.util.List;
import java.util.Map;

/**
 * フラグメント統計生成専用ユースケース - Inbound Port
 * 
 * 責務: フラグメント統計生成のみ
 * SRP準拠: 単一責任原則に従い、フラグメント統計生成のみを担当
 */
public interface FragmentStatisticsUseCase {

    /**
     * フラグメント統計情報生成
     */
    FragmentStatisticsResponse generateStatistics(List<FragmentSummary> fragments);

    /**
     * フラグメント統計レスポンス
     */
    class FragmentStatisticsResponse {
        private final Map<String, Long> templateStats;
        private final List<String> uniquePaths;

        public FragmentStatisticsResponse(Map<String, Long> templateStats, List<String> uniquePaths) {
            this.templateStats = templateStats;
            this.uniquePaths = uniquePaths;
        }

        public static FragmentStatisticsResponse success(Map<String, Long> templateStats, List<String> uniquePaths) {
            return new FragmentStatisticsResponse(templateStats, uniquePaths);
        }

        public Map<String, Long> getTemplateStats() { return templateStats; }
        public List<String> getUniquePaths() { return uniquePaths; }
    }
}
