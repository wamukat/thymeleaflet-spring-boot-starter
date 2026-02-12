package io.github.wamukat.thymeleaflet.domain.service;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * モデルキー名から推奨のサンプル値を推定するドメインサービス。
 */
@Component
public class ModelValueInferenceService {

    public Object inferLeafValue(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("is")
            || normalized.startsWith("has")
            || normalized.startsWith("can")
            || normalized.startsWith("should")
            || normalized.startsWith("enabled")
            || normalized.startsWith("active")) {
            return false;
        }
        if (normalized.contains("count")
            || normalized.contains("total")
            || normalized.contains("amount")
            || normalized.contains("price")
            || normalized.contains("point")
            || normalized.contains("score")
            || normalized.contains("num")
            || normalized.contains("size")
            || normalized.contains("balance")
            || normalized.contains("age")
            || normalized.contains("rate")
            || normalized.contains("percent")) {
            return 0;
        }
        if (normalized.contains("date") || normalized.contains("time")) {
            return "2026-01-01";
        }
        if (normalized.contains("email")) {
            return "sample@example.com";
        }
        return "Sample " + key;
    }
}
