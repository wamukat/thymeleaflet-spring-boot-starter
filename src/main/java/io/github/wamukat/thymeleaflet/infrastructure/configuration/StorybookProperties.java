package io.github.wamukat.thymeleaflet.infrastructure.configuration;

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

    // Getters and Setters
    
    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
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

    public void setResources(ResourceConfig resources) {
        this.resources = resources;
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
         * CSS読み込みパス (Shadow DOM用)
         * デフォルト: []
         */
        private List<String> stylesheets = new ArrayList<>();
        
        /**
         * キャッシュ期間（秒）
         * デフォルト: 3600
         */
        private int cacheDurationSeconds = 3600;

        // Getters and Setters
        
        public List<String> getTemplatePaths() {
            return templatePaths;
        }

        public void setTemplatePaths(List<String> templatePaths) {
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

        public void setStylesheets(List<String> stylesheets) {
            if (stylesheets != null && stylesheets.size() > 10) {
                throw new IllegalArgumentException("Maximum 10 stylesheets allowed");
            }
            this.stylesheets = stylesheets != null ? stylesheets : new ArrayList<>();
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
}