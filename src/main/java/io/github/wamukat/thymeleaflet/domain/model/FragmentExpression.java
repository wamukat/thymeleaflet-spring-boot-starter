package io.github.wamukat.thymeleaflet.domain.model;

import java.util.List;
import java.util.Objects;

public final class FragmentExpression {

    private final String templatePath;
    private final String fragmentName;
    private final List<String> arguments;
    private final boolean hasArgumentList;

    private FragmentExpression(
        String templatePath,
        String fragmentName,
        List<String> arguments,
        boolean hasArgumentList
    ) {
        if (templatePath.isBlank()) {
            throw new IllegalArgumentException("templatePath cannot be blank");
        }
        if (fragmentName.isBlank()) {
            throw new IllegalArgumentException("fragmentName cannot be blank");
        }
        this.templatePath = templatePath;
        this.fragmentName = fragmentName;
        this.arguments = List.copyOf(arguments);
        this.hasArgumentList = hasArgumentList;
    }

    public static FragmentExpression of(String templatePath, String fragmentName, List<String> arguments) {
        return of(templatePath, fragmentName, arguments, false);
    }

    public static FragmentExpression of(
        String templatePath,
        String fragmentName,
        List<String> arguments,
        boolean hasArgumentList
    ) {
        Objects.requireNonNull(templatePath, "templatePath cannot be null");
        Objects.requireNonNull(fragmentName, "fragmentName cannot be null");
        Objects.requireNonNull(arguments, "arguments cannot be null");
        return new FragmentExpression(templatePath.trim(), fragmentName.trim(), arguments, hasArgumentList);
    }

    public String templatePath() {
        return templatePath;
    }

    public String fragmentName() {
        return fragmentName;
    }

    public List<String> arguments() {
        return arguments;
    }

    public boolean hasArgumentList() {
        return hasArgumentList;
    }
}
