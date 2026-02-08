package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
        Locale locale = LocaleContextHolder.getLocale();
        for (StorybookProperties.ViewportPreset preset : presets) {
            Optional<String> id = normalize(preset.getId());
            Optional<Integer> width = Optional.ofNullable(preset.getWidth());
            Optional<Integer> height = Optional.ofNullable(preset.getHeight());
            String label = resolveLabel(preset, locale);
            if (id.isEmpty() || width.isEmpty() || height.isEmpty()) {
                continue;
            }
            options.add(new PreviewViewportOption(
                id.orElseThrow(),
                label,
                width.orElseThrow(),
                height.orElseThrow()
            ));
        }
        return options;
    }

    private String resolveLabel(StorybookProperties.ViewportPreset preset, Locale locale) {
        Optional<String> label = normalize(preset.getLabel());
        Optional<String> labelKey = normalize(preset.getLabelKey());
        if (label.isEmpty() && labelKey.isPresent()) {
            String key = labelKey.orElseThrow();
            return messageSource.getMessage(key, null, key, locale);
        }
        if (label.isEmpty()) {
            return normalize(preset.getId()).orElse("viewport");
        }
        return label.orElseThrow();
    }

    private Optional<String> normalize(@Nullable String value) {
        return Optional.ofNullable(value)
            .map(String::trim)
            .filter(trimmed -> !trimmed.isEmpty());
    }
}
