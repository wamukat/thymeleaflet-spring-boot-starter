package io.github.wamukat.thymeleaflet.application.port.inbound.fragment;

import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;

import java.util.List;
import java.util.Map;

/**
 * フラグメント階層構造構築専用ユースケース - Inbound Port
 * 
 * 責務: 階層構造構築のみ
 * SRP準拠: 単一責任原則に従い、フラグメント階層構造構築のみを担当
 */
public interface FragmentHierarchyUseCase {

    /**
     * フラグメント階層構造生成
     */
    FragmentHierarchyResponse buildHierarchicalStructure(List<FragmentDiscoveryService.FragmentInfo> fragments);

    /**
     * フラグメント階層レスポンス
     */
    class FragmentHierarchyResponse {
        private final Map<String, Object> hierarchicalStructure;

        public FragmentHierarchyResponse(Map<String, Object> hierarchicalStructure) {
            this.hierarchicalStructure = hierarchicalStructure;
        }

        public static FragmentHierarchyResponse success(Map<String, Object> hierarchicalStructure, int fragmentCount) {
            return new FragmentHierarchyResponse(hierarchicalStructure);
        }

        public Map<String, Object> getHierarchicalStructure() { return hierarchicalStructure; }
    }
}