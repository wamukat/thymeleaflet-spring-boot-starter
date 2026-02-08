package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
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
    private ResolvedStorybookConfig storybookConfig;

    @Autowired
    private MessageSource messageSource;

    public void applyPreviewConfig(Model model) {
        ResolvedStorybookConfig.PreviewConfig preview = storybookConfig.getPreview();
        model.addAttribute("previewViewportPresets", buildViewportOptions(preview.getViewports()));
        model.addAttribute("previewLightColor", preview.getBackgroundLight());
        model.addAttribute("previewDarkColor", preview.getBackgroundDark());
    }

    private List<PreviewViewportOption> buildViewportOptions(List<ResolvedStorybookConfig.ViewportPreset> presets) {
        List<PreviewViewportOption> options = new ArrayList<>();
        Locale locale = LocaleContextHolder.getLocale();
        for (ResolvedStorybookConfig.ViewportPreset preset : presets) {
            String label = resolveLabel(preset, locale);
            options.add(new PreviewViewportOption(
                preset.getId(),
                label,
                preset.getWidth(),
                preset.getHeight()
            ));
        }
        return options;
    }

    private String resolveLabel(ResolvedStorybookConfig.ViewportPreset preset, Locale locale) {
        String label = preset.getLabel().trim();
        String labelKey = preset.getLabelKey().trim();
        if (label.isEmpty() && !labelKey.isEmpty()) {
            String key = labelKey;
            return messageSource.getMessage(key, null, key, locale);
        }
        if (label.isEmpty()) {
            return preset.getId();
        }
        return label;
    }
}
