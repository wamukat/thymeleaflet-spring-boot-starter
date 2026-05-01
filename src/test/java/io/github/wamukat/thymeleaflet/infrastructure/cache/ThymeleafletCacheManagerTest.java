package io.github.wamukat.thymeleaflet.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.junit.jupiter.api.Test;

class ThymeleafletCacheManagerTest {

    @Test
    void getAndPut_shouldStoreValuesWhenCacheIsEnabled() {
        ThymeleafletCacheManager cacheManager = new ThymeleafletCacheManager(config(true));

        cacheManager.put("templates", "components/card", "content");

        assertThat(cacheManager.<String>get("templates", "components/card"))
            .contains("content");
    }

    @Test
    void getAndPut_shouldBypassStorageWhenCacheIsDisabled() {
        ThymeleafletCacheManager cacheManager = new ThymeleafletCacheManager(config(false));

        cacheManager.put("templates", "components/card", "content");

        assertThat(cacheManager.<String>get("templates", "components/card"))
            .isEmpty();
    }

    @Test
    void clear_shouldInvalidateOneCacheWithoutAffectingOthers() {
        ThymeleafletCacheManager cacheManager = new ThymeleafletCacheManager(config(true));
        cacheManager.put("templates", "components/card", "content");
        cacheManager.put("types", "components/card", "type info");

        cacheManager.clear("templates");

        assertThat(cacheManager.<String>get("templates", "components/card"))
            .isEmpty();
        assertThat(cacheManager.<String>get("types", "components/card"))
            .contains("type info");
    }

    @Test
    void clearAll_shouldInvalidateEveryCache() {
        ThymeleafletCacheManager cacheManager = new ThymeleafletCacheManager(config(true));
        cacheManager.put("templates", "components/card", "content");
        cacheManager.put("types", "components/card", "type info");

        cacheManager.clearAll();

        assertThat(cacheManager.<String>get("templates", "components/card"))
            .isEmpty();
        assertThat(cacheManager.<String>get("types", "components/card"))
            .isEmpty();
    }

    private ResolvedStorybookConfig config(boolean cacheEnabled) {
        StorybookProperties properties = new StorybookProperties();
        StorybookProperties.CacheConfig cache = new StorybookProperties.CacheConfig();
        cache.setEnabled(cacheEnabled);
        properties.setCache(cache);
        return ResolvedStorybookConfig.from(properties);
    }
}
