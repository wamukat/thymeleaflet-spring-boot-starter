package io.github.wamukat.thymeleaflet.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * テンプレート解析結果を保持し、推論モデルへ適用するドメインオブジェクト。
 */
public final class TemplateInference {

    private static final String INDEX_SEGMENT = "[]";

    private final List<ModelPath> modelPaths;
    private final Map<String, ModelPath> loopVariablePaths;
    private final Map<String, Boolean> referencedTemplatePaths;
    private final List<ModelPath> noArgMethodPaths;
    private final List<ReferencedFragment> referencedFragments;

    public TemplateInference(
        List<ModelPath> modelPaths,
        Map<String, ModelPath> loopVariablePaths,
        Map<String, Boolean> referencedTemplatePaths,
        List<ModelPath> noArgMethodPaths
    ) {
        this(modelPaths, loopVariablePaths, referencedTemplatePaths, noArgMethodPaths, List.of());
    }

    public TemplateInference(
        List<ModelPath> modelPaths,
        Map<String, ModelPath> loopVariablePaths,
        Map<String, Boolean> referencedTemplatePaths,
        List<ModelPath> noArgMethodPaths,
        List<ReferencedFragment> referencedFragments
    ) {
        this.modelPaths = List.copyOf(modelPaths);
        this.loopVariablePaths = Map.copyOf(new LinkedHashMap<>(loopVariablePaths));
        this.referencedTemplatePaths = Map.copyOf(new LinkedHashMap<>(referencedTemplatePaths));
        this.noArgMethodPaths = List.copyOf(noArgMethodPaths);
        this.referencedFragments = List.copyOf(referencedFragments);
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

    public List<ModelPath> noArgMethodPaths() {
        return noArgMethodPaths;
    }

    public List<ReferencedFragment> referencedFragments() {
        return referencedFragments;
    }

    public InferredModel toInferredModel() {
        InferredModel inferred = new InferredModel();
        for (ModelPath modelPath : modelPaths) {
            if (modelPath.isEmpty()) {
                continue;
            }
            ModelPath loopPath = loopVariablePaths.get(modelPath.root());
            int indexSegment = modelPath.segments().indexOf(INDEX_SEGMENT);
            if (indexSegment > 0) {
                List<String> iterablePath = loopPath == null
                    ? modelPath.segments().subList(0, indexSegment)
                    : loopPath.segments();
                List<String> itemSubPath = loopPath == null
                    ? indexedItemSubPath(modelPath, indexSegment)
                    : modelPath.segments().subList(1, modelPath.segments().size());
                inferred.putLoopPath(iterablePath, itemSubPath, modelPath.inferSampleValue());
                continue;
            }
            if (loopPath != null) {
                inferred.putLoopPath(loopPath.segments(), modelPath.subPathWithoutRoot(), modelPath.inferSampleValue());
                continue;
            }
            inferred.putPath(modelPath.segments(), modelPath.inferSampleValue());
        }
        return inferred;
    }

    private List<String> indexedItemSubPath(ModelPath modelPath, int indexSegment) {
        if (indexSegment >= modelPath.segments().size() - 1) {
            return List.of();
        }
        return modelPath.segments().subList(indexSegment + 1, modelPath.segments().size());
    }

    public record ReferencedFragment(
        String templatePath,
        String fragmentName,
        List<String> arguments,
        boolean hasArgumentList,
        boolean requiresChildModelRecursion
    ) {
        public ReferencedFragment {
            templatePath = templatePath.trim();
            fragmentName = fragmentName.trim();
            arguments = List.copyOf(arguments);
        }
    }
}
