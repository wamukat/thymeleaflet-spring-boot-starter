package io.github.wamukat.thymeleaflet.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * テンプレート解析で得られた推論情報のスナップショット。
 */
public record TemplateInferenceSnapshot(
    List<List<String>> modelPaths,
    Map<String, List<String>> loopVariablePaths,
    Set<String> referencedTemplatePaths
) {
}
