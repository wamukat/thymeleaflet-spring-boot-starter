package io.github.wamukat.thymeleaflet.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * モデル参照パスを表す値オブジェクト。
 */
public record ModelPath(List<String> segments) {

    private static final LocalDateTime SAMPLE_DATE_TIME = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final Pattern SUFFIXED_AT_PATTERN = Pattern.compile(".*[_-]at$");

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
        String leafName = leaf();
        String normalized = leafName.toLowerCase(Locale.ROOT);
        if (isBooleanLike(normalized)) {
            return false;
        }
        if (normalized.contains("message")) {
            return "Sample " + leafName;
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
        if (isDateTimeLike(leafName, normalized)) {
            return SAMPLE_DATE_TIME;
        }
        if (normalized.contains("date") || normalized.contains("time")) {
            return "2026-01-01";
        }
        if (normalized.contains("email")) {
            return "sample@example.com";
        }
        return "Sample " + leafName;
    }

    private static boolean isBooleanLike(String normalized) {
        return normalized.startsWith("is")
            || normalized.startsWith("has")
            || normalized.startsWith("can")
            || normalized.startsWith("should")
            || normalized.startsWith("enabled")
            || normalized.startsWith("active")
            || normalized.equals("read");
    }

    private static boolean isDateTimeLike(String leafName, String normalized) {
        return leafName.endsWith("At")
            || SUFFIXED_AT_PATTERN.matcher(normalized).matches()
            || normalized.contains("datetime")
            || normalized.contains("timestamp");
    }
}
