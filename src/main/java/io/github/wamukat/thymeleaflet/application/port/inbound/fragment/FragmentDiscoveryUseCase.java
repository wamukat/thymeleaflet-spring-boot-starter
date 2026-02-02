package io.github.wamukat.thymeleaflet.application.port.inbound.fragment;

import io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery.FragmentDiscoveryService;

/**
 * フラグメント発見専用ユースケース - Inbound Port
 * 
 * 責務: フラグメント発見のみ
 * SRP準拠: 単一責任原則に従い、フラグメント発見のみを担当
 */
public interface FragmentDiscoveryUseCase {

    /**
     * 特定フラグメント詳細発見処理
     */
    FragmentDetailResponse discoverFragment(String templatePath, String fragmentName);

    /**
     * フラグメント詳細レスポンス
     */
    class FragmentDetailResponse {
        private final FragmentDiscoveryService.FragmentInfo fragment;
        private final boolean found;
        private final String templatePath;
        private final String fragmentName;

        public FragmentDetailResponse(FragmentDiscoveryService.FragmentInfo fragment, boolean found, String templatePath, String fragmentName) {
            this.fragment = fragment;
            this.found = found;
            this.templatePath = templatePath;
            this.fragmentName = fragmentName;
        }

        public static FragmentDetailResponse success(FragmentDiscoveryService.FragmentInfo fragment, String templatePath, String fragmentName) {
            return new FragmentDetailResponse(fragment, fragment != null, templatePath, fragmentName);
        }

        public static FragmentDetailResponse notFound(String templatePath, String fragmentName) {
            return new FragmentDetailResponse(null, false, templatePath, fragmentName);
        }

        public FragmentDiscoveryService.FragmentInfo getFragment() { return fragment; }
        public boolean isFound() { return found; }
        public String getTemplatePath() { return templatePath; }
        public String getFragmentName() { return fragmentName; }
    }

}