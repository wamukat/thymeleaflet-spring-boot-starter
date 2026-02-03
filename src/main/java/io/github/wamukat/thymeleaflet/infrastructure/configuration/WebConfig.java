package io.github.wamukat.thymeleaflet.infrastructure.configuration;

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
        // パスパラメータでドット(.)が切り捨てられないようにする
        configurer.setUseSuffixPatternMatch(false);
        configurer.setUseTrailingSlashMatch(false);
        
        // パス変数内のスラッシュも含めてマッチングさせる
        // これにより shared/atoms/button-action のような複雑なパス構造に対応
        configurer.setUseRegisteredSuffixPatternMatch(false);
    }
}
