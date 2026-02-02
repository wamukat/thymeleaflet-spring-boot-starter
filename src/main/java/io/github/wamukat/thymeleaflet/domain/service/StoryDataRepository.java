package io.github.wamukat.thymeleaflet.domain.service;

import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;

import java.util.Map;

/**
 * ストーリーデータリポジトリ - Domain層インターフェース
 * 
 * Clean Architecture準拠:
 * - ドメイン層が定義するインターフェース
 * - Infrastructure層で実装される
 * - 依存関係逆転原理 (DIP) の適用
 */
public interface StoryDataRepository {
    
    /**
     * ストーリーパラメータを読み込み
     * 
     * @param storyInfo ストーリー情報
     * @return パラメータマップ
     */
    Map<String, Object> loadStoryParameters(FragmentStoryInfo storyInfo);
}