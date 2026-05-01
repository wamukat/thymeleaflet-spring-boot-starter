package io.github.wamukat.thymeleaflet.infrastructure.cache;

import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Central cache gateway for Thymeleaflet runtime-derived data.
 *
 * <p>When {@code thymeleaflet.cache.enabled=false}, all reads miss and writes are ignored. The
 * auto-configuration also disables this cache when {@code spring.thymeleaf.cache=false} unless
 * {@code thymeleaflet.cache.enabled} is explicitly set, so DevTools-style template reloads reread
 * source resources by default.
 */
@Component
public class ThymeleafletCacheManager {

    private final ResolvedStorybookConfig storybookConfig;
    private final Map<String, Map<Object, Object>> caches = new ConcurrentHashMap<>();

    public ThymeleafletCacheManager(ResolvedStorybookConfig storybookConfig) {
        this.storybookConfig = storybookConfig;
    }

    public boolean isEnabled() {
        return storybookConfig.getCache().isEnabled();
    }

    @SuppressWarnings("unchecked")
    public <V> Optional<V> get(String cacheName, Object key) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        return Optional.ofNullable((V) cache(cacheName).get(key));
    }

    public void put(String cacheName, Object key, Object value) {
        if (!isEnabled()) {
            return;
        }
        cache(cacheName).put(key, value);
    }

    public void clear(String cacheName) {
        caches.remove(cacheName);
    }

    public void clearAll() {
        caches.clear();
    }

    private Map<Object, Object> cache(String cacheName) {
        return caches.computeIfAbsent(cacheName, ignored -> new ConcurrentHashMap<>());
    }
}
