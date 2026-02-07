package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Storybook設定プロパティクラス
 * 
 * application.ymlまたはapplication.propertiesで設定可能な
 * Storybook関連のプロパティを定義します。
 */
@ConfigurationProperties(prefix = "thymeleaflet")
public class StorybookProperties {
    
    /**
     * StorybookのベースパスURL
     * デフォルト: /thymeleaflet
     */
    private String basePath = "/thymeleaflet";
    
    /**
     * デバッグモードの有効/無効
     * デフォルト: false
     */
    private boolean debug = false;
    
    /**
     * 統一リソース設定
     */
    private ResourceConfig resources = new ResourceConfig();

    /**
     * キャッシュ設定
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * プレビュー設定
     */
    private PreviewConfig preview = new PreviewConfig();

    // Getters and Setters
    
    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(@Nullable String basePath) {
        this.basePath = (basePath == null || basePath.isBlank()) ? "/thymeleaflet" : basePath;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public ResourceConfig getResources() {
        return resources;
    }

    public void setResources(@Nullable ResourceConfig resources) {
        this.resources = resources != null ? resources : new ResourceConfig();
    }

    public CacheConfig getCache() {
        return cache;
    }

    public void setCache(@Nullable CacheConfig cache) {
        this.cache = cache != null ? cache : new CacheConfig();
    }

    public PreviewConfig getPreview() {
        return preview;
    }

    public void setPreview(PreviewConfig preview) {
        this.preview = preview != null ? preview : new PreviewConfig();
    }

    /**
     * 統一リソース設定クラス
     */
    public static class ResourceConfig {
        
        /**
         * テンプレート読み込みパス
         * デフォルト: ["/templates/"]
         */
        private List<String> templatePaths = List.of("/templates/");
        
        /**
         * CSS読み込みパス (プレビュー用)
         * デフォルト: []
         */
        private List<String> stylesheets = new ArrayList<>();

        /**
         * JavaScript読み込みパス (プレビュー用)
         * デフォルト: []
         */
        private List<String> scripts = new ArrayList<>();
        
        /**
         * キャッシュ期間（秒）
         * デフォルト: 3600
         */
        private int cacheDurationSeconds = 3600;

        // Getters and Setters
        
        public List<String> getTemplatePaths() {
            return templatePaths;
        }

        public void setTemplatePaths(@Nullable List<String> templatePaths) {
            if (templatePaths == null || templatePaths.isEmpty()) {
                throw new IllegalArgumentException("At least one template path must be configured");
            }
            if (templatePaths.size() > 5) {
                throw new IllegalArgumentException("Maximum 5 template paths allowed");
            }
            this.templatePaths = templatePaths;
        }

        public List<String> getStylesheets() {
            return stylesheets;
        }

        public void setStylesheets(@Nullable List<String> stylesheets) {
            if (stylesheets != null && stylesheets.size() > 10) {
                throw new IllegalArgumentException("Maximum 10 stylesheets allowed");
            }
            this.stylesheets = stylesheets != null ? stylesheets : new ArrayList<>();
        }

        public List<String> getScripts() {
            return scripts;
        }

        public void setScripts(@Nullable List<String> scripts) {
            if (scripts != null && scripts.size() > 10) {
                throw new IllegalArgumentException("Maximum 10 scripts allowed");
            }
            this.scripts = scripts != null ? scripts : new ArrayList<>();
        }

        public int getCacheDurationSeconds() {
            return cacheDurationSeconds;
        }

        public void setCacheDurationSeconds(int cacheDurationSeconds) {
            if (cacheDurationSeconds < 1) {
                throw new IllegalArgumentException("Cache duration must be positive");
            }
            this.cacheDurationSeconds = cacheDurationSeconds;
        }
    }

    /**
     * キャッシュ設定クラス
     */
    public static class CacheConfig {
        /**
         * キャッシュ有効/無効
         */
        private boolean enabled = true;

        /**
         * 起動時のプリロード有効/無効
         */
        private boolean preload = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isPreload() {
            return preload;
        }

        public void setPreload(boolean preload) {
            this.preload = preload;
        }
    }

    /**
     * プレビュー設定クラス
     */
    public static class PreviewConfig {

        /**
         * 背景色（明るい）
         * デフォルト: #f3f4f6
         */
        private String backgroundLight = "#f3f4f6";

        /**
         * 背景色（暗い）
         * デフォルト: #1f2937
         */
        private String backgroundDark = "#1f2937";

        /**
         * ビューポート一覧（Fit以外）
         */
        private List<ViewportPreset> viewports = defaultViewports();

        public String getBackgroundLight() {
            return backgroundLight;
        }

        public void setBackgroundLight(@Nullable String backgroundLight) {
            this.backgroundLight = (backgroundLight == null || backgroundLight.isBlank())
                ? "#f3f4f6"
                : backgroundLight.trim();
        }

        public String getBackgroundDark() {
            return backgroundDark;
        }

        public void setBackgroundDark(@Nullable String backgroundDark) {
            this.backgroundDark = (backgroundDark == null || backgroundDark.isBlank())
                ? "#1f2937"
                : backgroundDark.trim();
        }

        public List<ViewportPreset> getViewports() {
            return viewports;
        }

        public void setViewports(@Nullable List<ViewportPreset> viewports) {
            if (viewports == null || viewports.isEmpty()) {
                this.viewports = new ArrayList<>();
                return;
            }
            if (viewports.size() > 10) {
                throw new IllegalArgumentException("Maximum 10 viewport presets allowed");
            }
            this.viewports = viewports;
        }

        private static List<ViewportPreset> defaultViewports() {
            List<ViewportPreset> presets = new ArrayList<>();
            presets.add(ViewportPreset.withLabelKey("mobileSmall", "thymeleaflet.preview.viewport.mobileSmall", 320, 568));
            presets.add(ViewportPreset.withLabelKey("mobileLarge", "thymeleaflet.preview.viewport.mobileLarge", 414, 896));
            presets.add(ViewportPreset.withLabelKey("tablet", "thymeleaflet.preview.viewport.tablet", 834, 1112));
            presets.add(ViewportPreset.withLabelKey("desktop", "thymeleaflet.preview.viewport.desktop", 1024, 1280));
            return presets;
        }
    }

    /**
     * ビューポートプリセット
     */
    public static class ViewportPreset {
        private @Nullable String id;
        private @Nullable String label;
        private @Nullable String labelKey;
        private @Nullable Integer width;
        private @Nullable Integer height;

        public ViewportPreset() {
        }

        public ViewportPreset(
            @Nullable String id,
            @Nullable String label,
            @Nullable String labelKey,
            @Nullable Integer width,
            @Nullable Integer height
        ) {
            this.id = id;
            this.label = label;
            this.labelKey = labelKey;
            this.width = width;
            this.height = height;
        }

        public static ViewportPreset withLabelKey(String id, String labelKey, Integer width, Integer height) {
            return new ViewportPreset(id, null, labelKey, width, height);
        }

        public @Nullable String getId() {
            return id;
        }

        public void setId(@Nullable String id) {
            this.id = id;
        }

        public @Nullable String getLabel() {
            return label;
        }

        public void setLabel(@Nullable String label) {
            this.label = label;
        }

        public @Nullable String getLabelKey() {
            return labelKey;
        }

        public void setLabelKey(@Nullable String labelKey) {
            this.labelKey = labelKey;
        }

        public @Nullable Integer getWidth() {
            return width;
        }

        public void setWidth(@Nullable Integer width) {
            this.width = width;
        }

        public @Nullable Integer getHeight() {
            return height;
        }

        public void setHeight(@Nullable Integer height) {
            this.height = height;
        }
    }
}
