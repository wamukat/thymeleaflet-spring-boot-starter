package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web設定クラス
 * 
 * パスパラメータの処理設定:
 * - ドット(.)を含むパスパラメータの処理を有効化
 * - スラッシュ(/)を含むパス変数の処理を有効化
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        invokeIfPresent(configurer, "setUseSuffixPatternMatch", false);
        invokeIfPresent(configurer, "setUseTrailingSlashMatch", false);
        invokeIfPresent(configurer, "setUseRegisteredSuffixPatternMatch", false);
    }

    private static void invokeIfPresent(PathMatchConfigurer configurer, String methodName, boolean value) {
        try {
            Method method = PathMatchConfigurer.class.getMethod(methodName, Boolean.class);
            method.invoke(configurer, value);
        } catch (NoSuchMethodException ignored) {
            // Spring Framework 7 (Boot 4) removed these legacy setters.
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to configure PathMatchConfigurer via " + methodName, e);
        }
    }
}
