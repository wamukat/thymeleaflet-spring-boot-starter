package io.github.wamukat.thymeleaflet.application.port.outbound;

import java.util.List;

/**
 * フラグメント依存情報の参照ポート。
 */
public interface FragmentDependencyPort {

    List<DependencyComponent> findDependenciesForView(String templatePath, String fragmentName);

    record DependencyComponent(String templatePath, String fragmentName, String encodedTemplatePath) {}
}
