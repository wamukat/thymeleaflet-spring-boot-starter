package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Sync LocaleContextHolder with the locale resolved by Spring MVC.
 */
public class ThymeleafletLocaleContextInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        Locale locale = RequestContextUtils.getLocale(request);
        LocaleContextHolder.setLocale(locale);
        return true;
    }
}
