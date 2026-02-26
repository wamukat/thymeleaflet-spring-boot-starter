package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypedValue;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Story model が Map の場合に no-arg メソッド呼び出しを key 参照へ変換する。
 */
public class MapNoArgMethodResolver implements MethodResolver {

    private static final Set<String> OBJECT_METHODS = Set.of(
        "toString",
        "hashCode",
        "equals",
        "wait",
        "notify",
        "notifyAll",
        "getClass"
    );

    private final MessageSource messageSource;

    public MapNoArgMethodResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public @Nullable MethodExecutor resolve(
        EvaluationContext context,
        Object targetObject,
        String name,
        List<TypeDescriptor> argumentTypes
    ) throws AccessException {
        if (!(targetObject instanceof Map<?, ?> mapTarget)) {
            return null;
        }
        if (argumentTypes != null && !argumentTypes.isEmpty()) {
            return null;
        }
        if (OBJECT_METHODS.contains(name) || hasZeroArgJavaMethod(mapTarget.getClass(), name)) {
            return null;
        }

        List<String> candidateKeys = candidateKeys(name);
        for (String candidate : candidateKeys) {
            if (mapTarget.containsKey(candidate)) {
                return (evaluationContext, target, arguments) ->
                    new TypedValue(((Map<?, ?>) target).get(candidate));
            }
        }

        Object fallbackValue = unresolvedFallbackValue(name);
        recordUnresolvedWarning(name, candidateKeys, mapTarget.keySet());
        return (evaluationContext, target, arguments) -> new TypedValue(fallbackValue);
    }

    private void recordUnresolvedWarning(String methodName, List<String> candidateKeys, Set<?> availableKeys) {
        Locale locale = LocaleContextHolder.getLocale();
        String candidatesText = String.join(", ", candidateKeys);
        String availableText = availableKeys.stream()
            .map(String::valueOf)
            .limit(5)
            .reduce((left, right) -> left + ", " + right)
            .orElse("-");

        String message = messageSource.getMessage(
            "thymeleaflet.preview.warning.unresolvedMethod",
            new Object[] {methodName + "()", candidatesText, availableText},
            "Unresolved method in preview: " + methodName + "()",
            locale
        );
        PreviewWarningRecorder.record(message);
    }

    private boolean hasZeroArgJavaMethod(Class<?> targetClass, String name) {
        try {
            Method method = targetClass.getMethod(name);
            return method.getParameterCount() == 0;
        } catch (NoSuchMethodException notFound) {
            return false;
        }
    }

    private List<String> candidateKeys(String methodName) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add(methodName);
        addDerivedKey(keys, methodName, "get");
        addDerivedKey(keys, methodName, "is");
        addDerivedKey(keys, methodName, "has");
        addDerivedKey(keys, methodName, "next");
        addDerivedKey(keys, methodName, "prev");
        return new ArrayList<>(keys);
    }

    private void addDerivedKey(Set<String> keys, String methodName, String prefix) {
        if (!methodName.startsWith(prefix) || methodName.length() <= prefix.length()) {
            return;
        }
        String suffix = methodName.substring(prefix.length());
        if (suffix.isEmpty()) {
            return;
        }
        String normalized = Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
        keys.add(normalized);
    }

    private Object unresolvedFallbackValue(String methodName) {
        String normalized = methodName.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("is")
            || normalized.startsWith("has")
            || normalized.startsWith("can")
            || normalized.startsWith("should")
            || normalized.startsWith("enabled")
            || normalized.startsWith("active")) {
            return false;
        }
        if (normalized.contains("count")
            || normalized.contains("total")
            || normalized.contains("amount")
            || normalized.contains("price")
            || normalized.contains("point")
            || normalized.contains("score")
            || normalized.contains("num")
            || normalized.contains("size")
            || normalized.contains("balance")
            || normalized.contains("age")
            || normalized.contains("rate")
            || normalized.contains("percent")
            || normalized.contains("page")
            || normalized.contains("index")
            || normalized.contains("offset")) {
            return 0;
        }
        if (normalized.contains("date") || normalized.contains("time")) {
            return "2026-01-01";
        }
        if (normalized.contains("email")) {
            return "sample@example.com";
        }
        return "Sample " + methodName;
    }
}
