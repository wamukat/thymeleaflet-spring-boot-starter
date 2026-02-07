package io.github.wamukat.thymeleaflet.application.port.inbound.fragment;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * メトリクス記録専用ユースケース - Inbound Port
 * 
 * 責務: メトリクス記録のみ
 * SRP準拠: 単一責任原則に従い、メトリクス記録のみを担当
 */
public interface MetricsUseCase {

    /**
     * 発見処理メトリクスのログ出力
     */
    void logDiscoveryMetrics(MetricsCommand command);

    /**
     * メトリクスコマンド
     */
    class MetricsCommand {
        private final @Nullable Map<String, Object> data;
        private final long discoveryTime;
        private final int fragmentCount;

        public MetricsCommand(Map<String, Object> data) {
            this.data = data;
            this.discoveryTime = 0;
            this.fragmentCount = 0;
        }

        public MetricsCommand(long discoveryTime, int fragmentCount) {
            this.discoveryTime = discoveryTime;
            this.fragmentCount = fragmentCount;
            this.data = null;
        }

        public @Nullable Map<String, Object> getData() { return data; }
        public long getDiscoveryTime() { return discoveryTime; }
        public int getFragmentCount() { return fragmentCount; }
    }

}
