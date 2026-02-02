package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Clean Architecture + DDD 復元プロジェクト用 Feature Toggle プロパティ
 * 
 * 段階的移行を制御するためのフラグ設定を管理する
 */
@Component
@ConfigurationProperties(prefix = "thymeleaflet.migration")
public class MigrationProperties {

    /**
     * 現在のマイグレーションフェーズ
     */
    private String phase = "4.0";

    /**
     * 監視・品質ゲート設定
     */
    private Monitoring monitoring = new Monitoring();

    // Getters and Setters

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Monitoring getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(Monitoring monitoring) {
        this.monitoring = monitoring;
    }

    /**
     * 監視・品質ゲート設定
     */
    public static class Monitoring {
        private int responseTimeDegradationThreshold = 10;
        private int errorRateIncreaseThreshold = 1;
        private boolean enforceContractTests = true;

        // Getters and Setters

        public int getResponseTimeDegradationThreshold() {
            return responseTimeDegradationThreshold;
        }

        public void setResponseTimeDegradationThreshold(int responseTimeDegradationThreshold) {
            this.responseTimeDegradationThreshold = responseTimeDegradationThreshold;
        }

        public int getErrorRateIncreaseThreshold() {
            return errorRateIncreaseThreshold;
        }

        public void setErrorRateIncreaseThreshold(int errorRateIncreaseThreshold) {
            this.errorRateIncreaseThreshold = errorRateIncreaseThreshold;
        }

        public boolean isEnforceContractTests() {
            return enforceContractTests;
        }

        public void setEnforceContractTests(boolean enforceContractTests) {
            this.enforceContractTests = enforceContractTests;
        }
    }

}
