package io.github.wamukat.thymeleaflet.infrastructure.adapter.story;

import io.github.wamukat.thymeleaflet.application.port.outbound.StoryDataPort;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.domain.service.StoryDataRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ストーリーデータリポジトリアダプター
 * 
 * Clean Architecture準拠:
 * - Domain層のインターフェースをInfrastructure層で実装
 * - Application層のPortを利用してデータアクセス
 * - 依存関係逆転原理 (DIP) の実装
 */
@Component
public class StoryDataRepositoryAdapter implements StoryDataRepository {
    
    private final StoryDataPort storyDataPort;
    
    public StoryDataRepositoryAdapter(StoryDataPort storyDataPort) {
        this.storyDataPort = storyDataPort;
    }
    
    @Override
    public Map<String, Object> loadStoryParameters(FragmentStoryInfo storyInfo) {
        return storyDataPort.loadStoryParameters(storyInfo);
    }
}