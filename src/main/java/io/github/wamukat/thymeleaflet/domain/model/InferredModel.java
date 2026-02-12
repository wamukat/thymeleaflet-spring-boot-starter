package io.github.wamukat.thymeleaflet.domain.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 推論モデルの構築とマージを担うドメインモデル。
 */
public final class InferredModel {

    private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

    public void putPath(List<String> path, Object leafValue) {
        if (path.isEmpty()) {
            return;
        }
        if (path.size() == 1) {
            values.putIfAbsent(path.getFirst(), leafValue);
            return;
        }

        Object rootObject = values.get(path.getFirst());
        Map<String, Object> rootMap;
        if (rootObject instanceof Map<?, ?> existingMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) existingMap;
            rootMap = casted;
        } else {
            rootMap = new LinkedHashMap<>();
            values.put(path.getFirst(), rootMap);
        }

        Map<String, Object> current = rootMap;
        for (int i = 1; i < path.size() - 1; i++) {
            String segment = path.get(i);
            Object child = current.get(segment);
            if (child instanceof Map<?, ?> childMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>) childMap;
                current = casted;
            } else {
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(segment, created);
                current = created;
            }
        }
        current.putIfAbsent(path.get(path.size() - 1), leafValue);
    }

    public void putLoopPath(List<String> iterablePath, List<String> itemSubPath, Object leafValue) {
        if (iterablePath == null || iterablePath.isEmpty()) {
            return;
        }
        if (iterablePath.size() == 1) {
            String key = iterablePath.getFirst();
            List<Object> list = ensureListValue(values, key);
            if (!itemSubPath.isEmpty()) {
                Map<String, Object> firstItem = ensureFirstListMap(list);
                putNestedMapPath(firstItem, itemSubPath, leafValue);
            }
            return;
        }
        Map<String, Object> current = values;
        for (int i = 0; i < iterablePath.size() - 1; i++) {
            String segment = iterablePath.get(i);
            Object child = current.get(segment);
            if (!(child instanceof Map<?, ?> childMap)) {
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(segment, created);
                current = created;
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) childMap;
            current = casted;
        }
        String listKey = iterablePath.get(iterablePath.size() - 1);
        List<Object> list = ensureListValue(current, listKey);
        if (!itemSubPath.isEmpty()) {
            Map<String, Object> firstItem = ensureFirstListMap(list);
            putNestedMapPath(firstItem, itemSubPath, leafValue);
        }
    }

    public void merge(InferredModel other) {
        deepMerge(values, other.values);
    }

    public Map<String, Object> toMap() {
        return values;
    }

    private void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object sourceValue = entry.getValue();
            Object targetValue = target.get(key);
            if (sourceValue instanceof Map<?, ?> sourceMap && targetValue instanceof Map<?, ?> targetMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> castedTarget = (Map<String, Object>) targetMap;
                @SuppressWarnings("unchecked")
                Map<String, Object> castedSource = (Map<String, Object>) sourceMap;
                deepMerge(castedTarget, castedSource);
                continue;
            }
            target.putIfAbsent(key, sourceValue);
        }
    }

    private List<Object> ensureListValue(Map<String, Object> parent, String key) {
        Object currentValue = parent.get(key);
        if (currentValue instanceof List<?> existingList) {
            @SuppressWarnings("unchecked")
            List<Object> casted = (List<Object>) existingList;
            return casted;
        }
        List<Object> created = new ArrayList<>();
        parent.put(key, created);
        return created;
    }

    private Map<String, Object> ensureFirstListMap(List<Object> list) {
        if (list.isEmpty()) {
            Map<String, Object> created = new LinkedHashMap<>();
            list.add(created);
            return created;
        }
        Object first = list.getFirst();
        if (first instanceof Map<?, ?> firstMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) firstMap;
            return casted;
        }
        Map<String, Object> created = new LinkedHashMap<>();
        list.set(0, created);
        return created;
    }

    private void putNestedMapPath(Map<String, Object> root, List<String> path, Object leafValue) {
        if (path.isEmpty()) {
            return;
        }
        if (path.size() == 1) {
            root.putIfAbsent(path.getFirst(), leafValue);
            return;
        }
        Map<String, Object> current = root;
        for (int i = 0; i < path.size() - 1; i++) {
            String segment = path.get(i);
            Object child = current.get(segment);
            if (!(child instanceof Map<?, ?> childMap)) {
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(segment, created);
                current = created;
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) childMap;
            current = casted;
        }
        current.putIfAbsent(path.get(path.size() - 1), leafValue);
    }
}
