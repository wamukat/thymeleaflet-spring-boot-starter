package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * /render レスポンス向けのプレビュー警告をリクエスト単位で記録する。
 */
public final class PreviewWarningRecorder {

    public static final String HEADER_NAME = "X-Thymeleaflet-Preview-Warnings";

    private static final String REQUEST_ATTRIBUTE_KEY =
        PreviewWarningRecorder.class.getName() + ".warnings";
    private static final int MAX_WARNINGS = 8;

    private PreviewWarningRecorder() {
    }

    public static void clear() {
        ServletRequestAttributes attributes = currentServletAttributes();
        if (attributes == null) {
            return;
        }
        attributes.getRequest().removeAttribute(REQUEST_ATTRIBUTE_KEY);
        HttpServletResponse response = attributes.getResponse();
        if (response != null) {
            response.setHeader(HEADER_NAME, "");
        }
    }

    public static void record(String warningMessage) {
        if (warningMessage == null || warningMessage.isBlank()) {
            return;
        }
        ServletRequestAttributes attributes = currentServletAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        List<String> warnings = getOrCreateWarnings(request);

        if (!warnings.contains(warningMessage) && warnings.size() < MAX_WARNINGS) {
            warnings.add(warningMessage);
        }

        HttpServletResponse response = attributes.getResponse();
        if (response != null) {
            response.setHeader(HEADER_NAME, encodeWarnings(warnings));
        }
    }

    private static List<String> getOrCreateWarnings(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ATTRIBUTE_KEY);
        if (existing instanceof List<?> existingList) {
            @SuppressWarnings("unchecked")
            List<String> casted = (List<String>) existingList;
            return casted;
        }
        List<String> warnings = new ArrayList<>();
        request.setAttribute(REQUEST_ATTRIBUTE_KEY, warnings);
        return warnings;
    }

    private static @Nullable ServletRequestAttributes currentServletAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes;
        }
        return null;
    }

    private static String encodeWarnings(List<String> warnings) {
        if (warnings.isEmpty()) {
            return "";
        }
        Set<String> unique = new LinkedHashSet<>(warnings);
        String joined = String.join("\n", unique);
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(joined.getBytes(StandardCharsets.UTF_8));
    }
}
