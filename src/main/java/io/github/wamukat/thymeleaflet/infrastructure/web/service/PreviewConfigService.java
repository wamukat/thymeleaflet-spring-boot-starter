package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * プレビュー設定を画面向けに整形するサービス
 */
@Component
public class PreviewConfigService {

    @Autowired
    private StorybookProperties storybookProperties;

    @Autowired
    private MessageSource messageSource;

    public void applyPreviewConfig(Model model) {
        StorybookProperties.PreviewConfig preview = storybookProperties.getPreview();
        model.addAttribute("previewViewportPresets", buildViewportOptions(preview.getViewports()));
        model.addAttribute("previewLightColor", preview.getBackgroundLight());
        model.addAttribute("previewDarkColor", preview.getBackgroundDark());
    }

    private List<PreviewViewportOption> buildViewportOptions(List<StorybookProperties.ViewportPreset> presets) {
        List<PreviewViewportOption> options = new ArrayList<>();
        if (presets == null) {
            return options;
        }
        Locale locale = LocaleContextHolder.getLocale();
        for (StorybookProperties.ViewportPreset preset : presets) {
            if (preset == null) {
                continue;
            }
            String id = safeString(preset.getId());
            Integer width = preset.getWidth();
            Integer height = preset.getHeight();
            String label = resolveLabel(preset, locale);
            if (id == null || width == null || height == null) {
                continue;
            }
            options.add(new PreviewViewportOption(id, label, width, height));
        }
        return options;
    }

    private String resolveLabel(StorybookProperties.ViewportPreset preset, Locale locale) {
        String label = safeString(preset.getLabel());
        String labelKey = safeString(preset.getLabelKey());
        if (label == null && labelKey != null) {
            return messageSource.getMessage(labelKey, null, labelKey, locale);
        }
        if (label == null) {
            return safeString(preset.getId());
        }
        return label;
    }

    private String safeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
