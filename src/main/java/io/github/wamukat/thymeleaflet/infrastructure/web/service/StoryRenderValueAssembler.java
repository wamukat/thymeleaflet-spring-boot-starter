package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.application.port.inbound.story.StoryParameterUseCase;
import io.github.wamukat.thymeleaflet.domain.model.FragmentStoryInfo;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import io.github.wamukat.thymeleaflet.infrastructure.web.rendering.PreviewWarningRecorder;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class StoryRenderValueAssembler {

    private final StoryParameterUseCase storyParameterUseCase;
    private final FragmentModelInferenceService fragmentModelInferenceService;
    private final StoryJavaTimeValueCoercionService storyJavaTimeValueCoercionService;
    private final MessageSource messageSource;
    private final JavaDocFallbackValueService javaDocFallbackValueService = new JavaDocFallbackValueService();

    StoryRenderValueAssembler(
        StoryParameterUseCase storyParameterUseCase,
        FragmentModelInferenceService fragmentModelInferenceService,
        StoryJavaTimeValueCoercionService storyJavaTimeValueCoercionService,
        MessageSource messageSource
    ) {
        this.storyParameterUseCase = storyParameterUseCase;
        this.fragmentModelInferenceService = fragmentModelInferenceService;
        this.storyJavaTimeValueCoercionService = storyJavaTimeValueCoercionService;
        this.messageSource = messageSource;
    }

    StoryRenderValues assemble(RenderValueAssemblyRequest request) {
        ModelAndMethodReturnValues modelValues = assembleModelAndMethodReturns(request);
        Map<String, Object> mergedParameters = assembleParameters(request);
        return new StoryRenderValues(modelValues.model(), mergedParameters, modelValues.methodReturns());
    }

    ModelAndMethodReturnValues assembleModelAndMethodReturns(RenderValueAssemblyRequest request) {
        FragmentStoryInfo storyInfo = request.storyInfo();
        Optional<JavaDocAnalyzer.JavaDocInfo> javaDocInfo = request.javaDocInfo();

        Map<String, Object> storyModel = storyInfo.getModel();
        if (storyModel.isEmpty()) {
            storyModel = fragmentModelInferenceService.inferModel(
                request.fullTemplatePath(),
                request.fragmentName(),
                storyInfo.getFragmentSummary().getParameters()
            );
            if (javaDocInfo.isPresent()) {
                Map<String, Object> javaDocModelDefaults =
                    javaDocFallbackValueService.modelDefaults(javaDocInfo.orElseThrow());
                if (!javaDocModelDefaults.isEmpty()) {
                    Map<String, Object> mergedFallbackModel = deepCopyMap(storyModel);
                    deepMergeWithOverride(mergedFallbackModel, javaDocModelDefaults);
                    storyModel = mergedFallbackModel;
                }
            }
        }

        Map<String, Object> mergedModel = new HashMap<>();
        if (!storyModel.isEmpty()) {
            deepMergeWithOverride(mergedModel, storyModel);
        }
        if (!request.modelOverrides().isEmpty()) {
            deepMergeWithOverride(mergedModel, request.modelOverrides());
        }

        Map<String, Object> mergedMethodReturns = new HashMap<>();
        if (!storyInfo.getMethodReturns().isEmpty()) {
            deepMergeWithOverride(mergedMethodReturns, storyInfo.getMethodReturns());
        }
        if (!request.methodReturnsOverrides().isEmpty()) {
            deepMergeWithOverride(mergedMethodReturns, request.methodReturnsOverrides());
        }
        if (mergedMethodReturns.isEmpty() && !storyInfo.hasStoryConfig()) {
            Map<String, Object> inferredMethodReturns = fragmentModelInferenceService.inferMethodReturnCandidates(
                request.fullTemplatePath(),
                request.fragmentName(),
                storyInfo.getFragmentSummary().getParameters()
            );
            if (!inferredMethodReturns.isEmpty()) {
                deepMergeWithOverride(mergedMethodReturns, inferredMethodReturns);
            }
        }

        if (!mergedMethodReturns.isEmpty()) {
            List<String> conflictPaths = new ArrayList<>();
            mergeMethodReturnsWithoutOverride(mergedModel, mergedMethodReturns, "", conflictPaths);
            conflictPaths.forEach(this::recordMethodReturnConflictWarning);
        }

        if (javaDocInfo.isPresent()) {
            mergedModel = storyJavaTimeValueCoercionService.coerceModel(
                mergedModel,
                javaDocInfo.orElseThrow()
            );
        }

        return new ModelAndMethodReturnValues(mergedModel, mergedMethodReturns);
    }

    Map<String, Object> assembleParameters(RenderValueAssemblyRequest request) {
        FragmentStoryInfo storyInfo = request.storyInfo();
        Optional<JavaDocAnalyzer.JavaDocInfo> javaDocInfo = request.javaDocInfo();

        Map<String, Object> parameters = storyParameterUseCase.getParametersForStory(storyInfo);
        if (!storyInfo.hasStoryConfig() && javaDocInfo.isPresent()) {
            Map<String, Object> javaDocParameterDefaults = javaDocFallbackValueService.parameterDefaults(
                javaDocInfo.orElseThrow(),
                request.fullTemplatePath(),
                request.fragmentName()
            );
            if (!javaDocParameterDefaults.isEmpty()) {
                Map<String, Object> mergedFallbackParameters = new HashMap<>(parameters);
                mergedFallbackParameters.putAll(javaDocParameterDefaults);
                parameters = mergedFallbackParameters;
            }
        }

        Map<String, Object> mergedParameters = new HashMap<>(parameters);
        if (!request.parameterOverrides().isEmpty()) {
            mergedParameters.putAll(request.parameterOverrides());
        }
        if (javaDocInfo.isPresent()) {
            mergedParameters = storyJavaTimeValueCoercionService.coerceParameters(
                mergedParameters,
                javaDocInfo.orElseThrow()
            );
        }

        return mergedParameters;
    }

    private void deepMergeWithOverride(Map<String, Object> target, Map<String, Object> source) {
        source.forEach((key, sourceValue) -> {
            Object targetValue = target.get(key);
            if (sourceValue instanceof Map<?, ?> sourceMap && targetValue instanceof Map<?, ?> targetMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> castedTarget = (Map<String, Object>) targetMap;
                deepMergeWithOverride(castedTarget, toStringKeyMap(sourceMap));
                return;
            }
            target.put(key, deepCopyValue(sourceValue));
        });
    }

    private void mergeMethodReturnsWithoutOverride(
        Map<String, Object> target,
        Map<String, Object> methodReturns,
        String parentPath,
        List<String> conflictPaths
    ) {
        methodReturns.forEach((key, sourceValue) -> {
            String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;
            Object targetValue = target.get(key);

            if (sourceValue instanceof Map<?, ?> sourceMap) {
                if (!target.containsKey(key)) {
                    target.put(key, deepCopyValue(sourceValue));
                    return;
                }
                if (targetValue instanceof Map<?, ?> targetMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> castedTarget = (Map<String, Object>) targetMap;
                    mergeMethodReturnsWithoutOverride(
                        castedTarget,
                        toStringKeyMap(sourceMap),
                        currentPath,
                        conflictPaths
                    );
                    return;
                }
                conflictPaths.add(currentPath);
                return;
            }

            if (target.containsKey(key)) {
                conflictPaths.add(currentPath);
                return;
            }
            target.put(key, deepCopyValue(sourceValue));
        });
    }

    private void recordMethodReturnConflictWarning(String path) {
        String warning = messageSource.getMessage(
            "thymeleaflet.preview.warning.methodReturnConflict",
            new Object[] {path},
            "Skipped methodReturns path due to model conflict: " + path,
            LocaleContextHolder.getLocale()
        );
        PreviewWarningRecorder.record(warning);
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> rawMap) {
        Map<String, Object> converted = new HashMap<>();
        rawMap.forEach((key, value) -> converted.put(String.valueOf(key), deepCopyValue(value)));
        return converted;
    }

    private Map<String, Object> deepCopyMap(Map<String, Object> source) {
        Map<String, Object> copied = new HashMap<>();
        source.forEach((key, value) -> copied.put(key, deepCopyValue(value)));
        return copied;
    }

    private @Nullable Object deepCopyValue(@Nullable Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            return toStringKeyMap(mapValue);
        }
        if (value instanceof List<?> listValue) {
            List<Object> copied = new ArrayList<>(listValue.size());
            listValue.forEach(item -> copied.add(deepCopyValue(item)));
            return copied;
        }
        return value;
    }

    record RenderValueAssemblyRequest(
        FragmentStoryInfo storyInfo,
        Optional<JavaDocAnalyzer.JavaDocInfo> javaDocInfo,
        String fullTemplatePath,
        String fragmentName,
        Map<String, Object> parameterOverrides,
        Map<String, Object> modelOverrides,
        Map<String, Object> methodReturnsOverrides
    ) {
    }

    record StoryRenderValues(
        Map<String, Object> model,
        Map<String, Object> parameters,
        Map<String, Object> methodReturns
    ) {
    }

    record ModelAndMethodReturnValues(
        Map<String, Object> model,
        Map<String, Object> methodReturns
    ) {
    }
}
