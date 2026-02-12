package io.github.wamukat.thymeleaflet.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * テンプレート解析結果を保持し、推論モデルへ適用するドメインオブジェクト。
 */
public final class TemplateInference {

    private final List<ModelPath> modelPaths;
    private final Map<String, ModelPath> loopVariablePaths;
    private final Map<String, Boolean> referencedTemplatePaths;

    public TemplateInference(
        List<ModelPath> modelPaths,
        Map<String, ModelPath> loopVariablePaths,
        Map<String, Boolean> referencedTemplatePaths
    ) {
        this.modelPaths = List.copyOf(modelPaths);
        this.loopVariablePaths = Map.copyOf(new LinkedHashMap<>(loopVariablePaths));
        this.referencedTemplatePaths = Map.copyOf(new LinkedHashMap<>(referencedTemplatePaths));
    }

    public List<ModelPath> modelPaths() {
        return modelPaths;
    }

    public Map<String, ModelPath> loopVariablePaths() {
        return loopVariablePaths;
    }

    public Set<String> referencedTemplatePaths() {
        return referencedTemplatePaths.keySet();
    }

    public Map<String, Boolean> referencedTemplatePathsWithRecursionFlags() {
        return referencedTemplatePaths;
    }

    public InferredModel toInferredModel() {
        InferredModel inferred = new InferredModel();
        for (ModelPath modelPath : modelPaths) {
            if (modelPath.isEmpty()) {
                continue;
            }
            ModelPath loopPath = loopVariablePaths.get(modelPath.root());
            if (loopPath != null) {
                inferred.putLoopPath(loopPath.segments(), modelPath.subPathWithoutRoot(), modelPath.inferSampleValue());
                continue;
            }
            inferred.putPath(modelPath.segments(), modelPath.inferSampleValue());
        }
        return inferred;
    }
}
