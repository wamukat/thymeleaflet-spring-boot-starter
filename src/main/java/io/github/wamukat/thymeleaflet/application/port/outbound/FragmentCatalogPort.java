package io.github.wamukat.thymeleaflet.application.port.outbound;

import io.github.wamukat.thymeleaflet.domain.model.FragmentSummary;

import java.util.List;
import java.util.Optional;

/**
 * フラグメント一覧取得ポート。
 */
public interface FragmentCatalogPort {

    List<FragmentSummary> discoverFragments();

    default Optional<FragmentSummary> findFragment(String templatePath, String fragmentName) {
        return discoverFragments().stream()
            .filter(fragment -> fragment.getTemplatePath().equals(templatePath))
            .filter(fragment -> fragment.getFragmentName().equals(fragmentName))
            .findFirst();
    }
}

