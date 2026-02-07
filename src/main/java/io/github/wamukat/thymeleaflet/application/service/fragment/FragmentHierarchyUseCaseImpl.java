package io.github.wamukat.thymeleaflet.application.service.fragment;

import io.github.wamukat.thymeleaflet.application.port.inbound.fragment.FragmentHierarchyUseCase;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * フラグメント階層構造構築専用ユースケース実装
 * 
 * 責務: 階層構造構築のみ
 * SRP準拠: 単一責任原則に従い、フラグメント階層構造構築のみを担当
 */
@Component
@Transactional(readOnly = true)
public class FragmentHierarchyUseCaseImpl implements FragmentHierarchyUseCase {

    @Override
    public FragmentHierarchyResponse buildHierarchicalStructure(List<FragmentDiscoveryService.FragmentInfo> fragments) {
        // 階層構造構築 - FragmentQueryService統合
        Map<String, Object> hierarchicalStructure = buildFragmentHierarchy(fragments);
        
        return FragmentHierarchyResponse.success(hierarchicalStructure, fragments.size());
    }

    /**
     * 階層構造構築 (FragmentQueryService統合機能) - 元の構造を保持して辞書順ソート対応
     */
    private Map<String, Object> buildFragmentHierarchy(List<FragmentDiscoveryService.FragmentInfo> fragments) {
        // 辞書順を保持するためLinkedHashMapを使用
        Map<String, Object> hierarchy = new LinkedHashMap<>();
        
        // フラグメントを辞書順でソート
        List<FragmentDiscoveryService.FragmentInfo> sortedFragments = fragments.stream()
            .sorted(Comparator.comparing(FragmentDiscoveryService.FragmentInfo::getTemplatePath)
                   .thenComparing(FragmentDiscoveryService.FragmentInfo::getFragmentName))
            .collect(Collectors.toList());
        
        for (FragmentDiscoveryService.FragmentInfo fragment : sortedFragments) {
            String[] pathParts = fragment.getTemplatePath().split("/");
            Map<String, Object> currentLevel = hierarchy;
            
            for (int i = 0; i < pathParts.length; i++) {
                String part = pathParts[i];
                
                if (i == pathParts.length - 1) {
                    // 最後のレベル: _fragmentsマップにフラグメントを追加
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fragmentsMap = (Map<String, Object>) currentLevel.computeIfAbsent("_fragments", k -> new LinkedHashMap<>());
                    
                    @SuppressWarnings("unchecked")
                    List<FragmentDiscoveryService.FragmentInfo> fragmentList = 
                        (List<FragmentDiscoveryService.FragmentInfo>) fragmentsMap.computeIfAbsent(part, k -> new ArrayList<>());
                    fragmentList.add(fragment);
                } else {
                    // 中間レベル: 階層を作成
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nextLevel = (Map<String, Object>) currentLevel.computeIfAbsent(part, k -> new LinkedHashMap<>());
                    currentLevel = nextLevel;
                }
            }
        }
        
        // 各階層レベルで辞書順ソートを適用
        return sortHierarchyRecursively(hierarchy);
    }
    
    /**
     * 階層構造を再帰的に辞書順でソート
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sortHierarchyRecursively(Map<String, Object> hierarchy) {
        Map<String, Object> sortedHierarchy = new LinkedHashMap<>();
        
        // キーを辞書順でソート（_で始まるメタデータは最後に配置）
        List<String> sortedKeys = hierarchy.keySet().stream()
            .sorted((key1, key2) -> {
                // _で始まるキーは最後に配置
                if (key1.startsWith("_") && !key2.startsWith("_")) {
                    return 1;
                } else if (!key1.startsWith("_") && key2.startsWith("_")) {
                    return -1;
                } else {
                    return key1.compareTo(key2);
                }
            })
            .collect(Collectors.toList());
        
        // ソートされた順序で再構築
        for (String key : sortedKeys) {
            Object value = hierarchy.get(key);
            
            // ネストしたMapがある場合は再帰的にソート
            if (value instanceof Map && !key.startsWith("_")) {
                sortedHierarchy.put(key, sortHierarchyRecursively((Map<String, Object>) value));
            } else if (value instanceof Map && "_fragments".equals(key)) {
                // _fragmentsマップ内も辞書順でソート
                Map<String, Object> fragmentsMap = (Map<String, Object>) value;
                Map<String, Object> sortedFragmentsMap = new LinkedHashMap<>();
                
                fragmentsMap.keySet().stream()
                    .sorted()
                    .forEach(fragmentKey -> {
                        List<FragmentDiscoveryService.FragmentInfo> fragmentList = 
                            (List<FragmentDiscoveryService.FragmentInfo>) fragmentsMap.get(fragmentKey);
                        if (fragmentList == null) {
                            sortedFragmentsMap.put(fragmentKey, List.of());
                            return;
                        }
                        // フラグメントリスト内も名前でソート
                        List<FragmentDiscoveryService.FragmentInfo> sortedList = fragmentList.stream()
                            .sorted(Comparator.comparing(FragmentDiscoveryService.FragmentInfo::getFragmentName))
                            .collect(Collectors.toList());
                        sortedFragmentsMap.put(fragmentKey, sortedList);
                    });
                
                sortedHierarchy.put(key, sortedFragmentsMap);
            } else {
                sortedHierarchy.put(key, value);
            }
        }
        
        return sortedHierarchy;
    }
}
