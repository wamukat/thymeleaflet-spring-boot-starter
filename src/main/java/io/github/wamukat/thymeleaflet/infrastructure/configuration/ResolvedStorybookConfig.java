package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 実行時に利用する非null前提の設定オブジェクト。
 *
 * <p>StorybookProperties(バインド入力)から変換し、ここで検証を完了させる。</p>
 */
public final class ResolvedStorybookConfig {

    private static final String DEFAULT_BASE_PATH = "/thymeleaflet";
    private static final String DEFAULT_BACKGROUND_LIGHT = "#f3f4f6";
    private static final String DEFAULT_BACKGROUND_DARK = "#1f2937";

    private final String basePath;
    private final boolean debug;
    private final ResourceConfig resources;
    private final CacheConfig cache;
    private final PreviewConfig preview;
    private final SecurityConfig security;

    private ResolvedStorybookConfig(
        String basePath,
        boolean debug,
        ResourceConfig resources,
        CacheConfig cache,
        PreviewConfig preview,
        SecurityConfig security
    ) {
        this.basePath = Objects.requireNonNull(basePath, "basePath cannot be null");
        this.debug = debug;
        this.resources = Objects.requireNonNull(resources, "resources cannot be null");
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
        this.preview = Objects.requireNonNull(preview, "preview cannot be null");
        this.security = Objects.requireNonNull(security, "security cannot be null");
    }

    public static ResolvedStorybookConfig from(StorybookProperties raw) {
        Objects.requireNonNull(raw, "raw cannot be null");
        String basePath = normalizeBasePath(raw.getBasePath());
        if (!DEFAULT_BASE_PATH.equals(basePath)) {
            throw new IllegalArgumentException(
                "Only '/thymeleaflet' is currently supported for thymeleaflet.base-path. Configured value: " + basePath
            );
        }
        StorybookProperties.ResourceConfig rawResources = raw.getResources();
        StorybookProperties.CacheConfig rawCache = raw.getCache();
        StorybookProperties.PreviewConfig rawPreview = raw.getPreview();
        StorybookProperties.SecurityConfig rawSecurity = raw.getSecurity();

        ResourceConfig resources = ResourceConfig.from(
            rawResources != null ? rawResources : new StorybookProperties.ResourceConfig()
        );
        CacheConfig cache = CacheConfig.from(
            rawCache != null ? rawCache : new StorybookProperties.CacheConfig()
        );
        PreviewConfig preview = PreviewConfig.from(
            rawPreview != null ? rawPreview : new StorybookProperties.PreviewConfig()
        );
        SecurityConfig security = SecurityConfig.from(
            rawSecurity != null ? rawSecurity : new StorybookProperties.SecurityConfig()
        );
        return new ResolvedStorybookConfig(basePath, raw.isDebug(), resources, cache, preview, security);
    }

    public String getBasePath() {
        return basePath;
    }

    public boolean isDebug() {
        return debug;
    }

    public ResourceConfig getResources() {
        return resources;
    }

    public CacheConfig getCache() {
        return cache;
    }

    public PreviewConfig getPreview() {
        return preview;
    }

    public SecurityConfig getSecurity() {
        return security;
    }

    public static final class ResourceConfig {
        private final List<String> templatePaths;
        private final List<String> stylesheets;
        private final List<String> scripts;
        private final int cacheDurationSeconds;

        private ResourceConfig(
            List<String> templatePaths,
            List<String> stylesheets,
            List<String> scripts,
            int cacheDurationSeconds
        ) {
            this.templatePaths = List.copyOf(templatePaths);
            this.stylesheets = List.copyOf(stylesheets);
            this.scripts = List.copyOf(scripts);
            this.cacheDurationSeconds = cacheDurationSeconds;
        }

        private static ResourceConfig from(StorybookProperties.ResourceConfig source) {
            List<String> templatePaths = sanitizeNonBlankList(source.getTemplatePaths(), "templatePaths");
            if (templatePaths.isEmpty()) {
                throw new IllegalArgumentException("At least one template path must be configured");
            }
            if (templatePaths.size() > 5) {
                throw new IllegalArgumentException("Maximum 5 template paths allowed");
            }

            List<String> stylesheets = sanitizeNonBlankList(source.getStylesheets(), "stylesheets");
            if (stylesheets.size() > 10) {
                throw new IllegalArgumentException("Maximum 10 stylesheets allowed");
            }

            List<String> scripts = sanitizeNonBlankList(source.getScripts(), "scripts");
            if (scripts.size() > 10) {
                throw new IllegalArgumentException("Maximum 10 scripts allowed");
            }

            int cacheDurationSeconds = source.getCacheDurationSeconds();
            if (cacheDurationSeconds < 1) {
                throw new IllegalArgumentException("Cache duration must be positive");
            }

            return new ResourceConfig(templatePaths, stylesheets, scripts, cacheDurationSeconds);
        }

        public List<String> getTemplatePaths() {
            return templatePaths;
        }

        public List<String> getStylesheets() {
            return stylesheets;
        }

        public List<String> getScripts() {
            return scripts;
        }

        public int getCacheDurationSeconds() {
            return cacheDurationSeconds;
        }
    }

    public static final class CacheConfig {
        private final boolean enabled;
        private final boolean preload;

        private CacheConfig(boolean enabled, boolean preload) {
            this.enabled = enabled;
            this.preload = preload;
        }

        private static CacheConfig from(StorybookProperties.CacheConfig source) {
            return new CacheConfig(source.isEnabled(), source.isPreload());
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isPreload() {
            return preload;
        }
    }

    public static final class PreviewConfig {
        private final String backgroundLight;
        private final String backgroundDark;
        private final List<ViewportPreset> viewports;

        private PreviewConfig(String backgroundLight, String backgroundDark, List<ViewportPreset> viewports) {
            this.backgroundLight = backgroundLight;
            this.backgroundDark = backgroundDark;
            this.viewports = List.copyOf(viewports);
        }

        private static PreviewConfig from(StorybookProperties.PreviewConfig source) {
            String backgroundLight = normalizeOrDefault(source.getBackgroundLight(), DEFAULT_BACKGROUND_LIGHT);
            String backgroundDark = normalizeOrDefault(source.getBackgroundDark(), DEFAULT_BACKGROUND_DARK);

            List<ViewportPreset> viewports = new ArrayList<>();
            List<StorybookProperties.ViewportPreset> rawViewports = source.getViewports();
            if (rawViewports != null) {
                for (StorybookProperties.ViewportPreset preset : rawViewports) {
                    if (preset == null) {
                        throw new IllegalArgumentException("Viewport preset cannot be null");
                    }
                    viewports.add(ViewportPreset.from(preset));
                }
            }
            if (viewports.size() > 10) {
                throw new IllegalArgumentException("Maximum 10 viewport presets allowed");
            }
            return new PreviewConfig(backgroundLight, backgroundDark, viewports);
        }

        public String getBackgroundLight() {
            return backgroundLight;
        }

        public String getBackgroundDark() {
            return backgroundDark;
        }

        public List<ViewportPreset> getViewports() {
            return viewports;
        }
    }

    public static final class SecurityConfig {
        private final boolean autoPermit;

        private SecurityConfig(boolean autoPermit) {
            this.autoPermit = autoPermit;
        }

        private static SecurityConfig from(StorybookProperties.SecurityConfig source) {
            return new SecurityConfig(source.isAutoPermit());
        }

        public boolean isAutoPermit() {
            return autoPermit;
        }
    }

    public static final class ViewportPreset {
        private final String id;
        private final String label;
        private final String labelKey;
        private final int width;
        private final int height;

        private ViewportPreset(String id, String label, String labelKey, int width, int height) {
            this.id = id;
            this.label = label;
            this.labelKey = labelKey;
            this.width = width;
            this.height = height;
        }

        private static ViewportPreset from(StorybookProperties.ViewportPreset raw) {
            String id = normalizeOrDefault(raw.getId(), "");
            if (id.isEmpty()) {
                throw new IllegalArgumentException("Viewport id must not be blank");
            }

            Integer width = raw.getWidth();
            Integer height = raw.getHeight();
            if (width == null || width <= 0) {
                throw new IllegalArgumentException("Viewport width must be positive for id: " + id);
            }
            if (height == null || height <= 0) {
                throw new IllegalArgumentException("Viewport height must be positive for id: " + id);
            }

            return new ViewportPreset(
                id,
                normalizeOrDefault(raw.getLabel(), ""),
                normalizeOrDefault(raw.getLabelKey(), ""),
                width,
                height
            );
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public String getLabelKey() {
            return labelKey;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    private static String normalizeOrDefault(@Nullable String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? defaultValue : normalized;
    }

    private static String normalizeBasePath(@Nullable String value) {
        String normalized = normalizeOrDefault(value, DEFAULT_BASE_PATH);
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static List<String> sanitizeNonBlankList(@Nullable List<String> values, String fieldName) {
        List<String> source = values != null ? values : List.of();
        List<String> normalized = new ArrayList<>();
        for (String value : source) {
            if (value == null) {
                throw new IllegalArgumentException(fieldName + " cannot contain null values");
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }
}
