package io.github.wamukat.thymeleaflet.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * モデル参照パスを表す値オブジェクト。
 */
public record ModelPath(List<String> segments) {

    public ModelPath {
        segments = List.copyOf(segments);
    }

    public static ModelPath of(List<String> segments) {
        return new ModelPath(segments);
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public String root() {
        return segments.getFirst();
    }

    public String leaf() {
        return segments.getLast();
    }

    public List<String> subPathWithoutRoot() {
        if (segments.size() <= 1) {
            return List.of();
        }
        return new ArrayList<>(segments.subList(1, segments.size()));
    }

    public Object inferSampleValue() {
        String normalized = leaf().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("is")
            || normalized.startsWith("has")
            || normalized.startsWith("can")
            || normalized.startsWith("should")
            || normalized.startsWith("enabled")
            || normalized.startsWith("active")) {
            return false;
        }
        if (normalized.contains("message")) {
            return "Sample " + leaf();
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
        return "Sample " + leaf();
    }
}
