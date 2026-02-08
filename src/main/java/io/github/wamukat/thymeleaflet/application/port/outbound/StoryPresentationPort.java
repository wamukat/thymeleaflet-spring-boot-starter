package io.github.wamukat.thymeleaflet.application.port.outbound;

import org.springframework.ui.Model;

import java.util.Map;

/**
 * Story画面表示向けの技術的モデル設定を提供するポート。
 */
public interface StoryPresentationPort {

    Map<String, Object> configureModelWithStoryParameters(Map<String, Object> storyParameters, Model model);
}

