package io.github.wamukat.thymeleaflet.domain.service;

import io.github.wamukat.thymeleaflet.domain.model.ModelPath;
import io.github.wamukat.thymeleaflet.domain.model.FragmentExpression;
import io.github.wamukat.thymeleaflet.domain.model.TemplateInference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * テンプレート式を解析し、モデル推論に必要な情報を抽出する。
 */
public class TemplateModelExpressionAnalyzer {

    private final StructuredTemplateParser templateParser;
    private final FragmentExpressionParser fragmentExpressionParser;

    public TemplateModelExpressionAnalyzer() {
        this(new StructuredTemplateParser(), new FragmentExpressionParser());
    }

    TemplateModelExpressionAnalyzer(StructuredTemplateParser templateParser) {
        this(templateParser, new FragmentExpressionParser());
    }

    TemplateModelExpressionAnalyzer(
        StructuredTemplateParser templateParser,
        FragmentExpressionParser fragmentExpressionParser
    ) {
        this.templateParser = templateParser;
        this.fragmentExpressionParser = fragmentExpressionParser;
    }

    public TemplateInference analyze(String html, Set<String> parameterNames) {
        return analyze(html, parameterNames, Optional.empty());
    }

    public TemplateInference analyze(String html, Set<String> parameterNames, String currentTemplatePath) {
        if (currentTemplatePath == null || currentTemplatePath.isBlank()) {
            return analyze(html, parameterNames, Optional.empty());
        }
        return analyze(html, parameterNames, Optional.of(currentTemplatePath.trim()));
    }

    private TemplateInference analyze(
        String html,
        Set<String> parameterNames,
        Optional<String> currentTemplatePath
    ) {
        StructuredTemplateParser.ParsedTemplate template = templateParser.parse(html);
        Set<String> excludedIdentifiers = new HashSet<>(parameterNames);
        excludedIdentifiers.addAll(extractLocalVariablesFromThWith(template));
        Map<String, ModelPath> loopVariablePaths = extractLoopVariablePaths(template, excludedIdentifiers);
        List<ExpressionSource> expressionSources = expressionSources(template, excludedIdentifiers);
        List<ModelPath> modelPaths = extractModelPathsFromSources(expressionSources, excludedIdentifiers);
        List<ModelPath> noArgMethodPaths = extractNoArgMethodPathsFromSources(expressionSources, excludedIdentifiers);
        List<TemplateInference.ReferencedFragment> referencedFragments =
            extractReferencedFragments(template, currentTemplatePath);
        Map<String, Boolean> referencedTemplatePaths = referencedTemplatePaths(referencedFragments);
        Map<ModelPath, Object> representativeValues = extractSwitchCaseRepresentativeValues(
            template,
            excludedIdentifiers
        );
        return new TemplateInference(
            modelPaths,
            loopVariablePaths,
            referencedTemplatePaths,
            noArgMethodPaths,
            referencedFragments,
            representativeValues
        );
    }

    private List<ModelPath> extractModelPathsFromSources(
        List<ExpressionSource> sources,
        Set<String> excludedIdentifiers
    ) {
        List<ModelPath> paths = new ArrayList<>();
        for (ExpressionSource source : sources) {
            for (TemplateExpression expression : extractExpressionBodies(source.value())) {
                if (expression.selectionExpression() && source.selectionRootExcluded()) {
                    continue;
                }
                for (List<String> path : extractModelPaths(expression.content(), excludedIdentifiers)) {
                    if (expression.selectionExpression() && source.selectionRoot().isPresent()) {
                        path = selectedPath(source.selectionRoot().orElseThrow(), path);
                    }
                    ModelPath modelPath = ModelPath.of(path);
                    if (!isLoopStatusPath(modelPath, source.loopStatusAliases())) {
                        paths.add(modelPath);
                    }
                }
            }
        }
        return paths;
    }

    private List<ModelPath> extractNoArgMethodPathsFromSources(
        List<ExpressionSource> sources,
        Set<String> excludedIdentifiers
    ) {
        LinkedHashSet<ModelPath> methodPaths = new LinkedHashSet<>();
        for (ExpressionSource source : sources) {
            for (TemplateExpression expression : extractExpressionBodies(source.value())) {
                if (expression.selectionExpression() && source.selectionRootExcluded()) {
                    continue;
                }
                List<List<String>> extracted = new ArrayList<>();
                extractModelPaths(expression.content(), excludedIdentifiers, extracted);
                for (List<String> path : extracted) {
                    if (expression.selectionExpression() && source.selectionRoot().isPresent()) {
                        path = selectedPath(source.selectionRoot().orElseThrow(), path);
                    }
                    ModelPath modelPath = ModelPath.of(path);
                    if (!isLoopStatusPath(modelPath, source.loopStatusAliases())) {
                        methodPaths.add(modelPath);
                    }
                }
            }
        }
        return new ArrayList<>(methodPaths);
    }

    private List<String> selectedPath(ModelPath selectionRoot, List<String> selectionPath) {
        if (selectionPath.isEmpty()) {
            return selectionRoot.segments();
        }
        List<String> path = new ArrayList<>(selectionRoot.segments());
        path.addAll(selectionPath);
        return path;
    }

    private List<TemplateExpression> extractExpressionBodies(String source) {
        List<TemplateExpression> expressions = new ArrayList<>();
        int index = 0;
        while (index < source.length() - 1) {
            char current = source.charAt(index);
            if ((current != '$' && current != '*') || source.charAt(index + 1) != '{') {
                index++;
                continue;
            }
            Optional<ExpressionBody> expressionBody = readExpressionBody(source, index + 2);
            if (expressionBody.isEmpty()) {
                index += 2;
                continue;
            }
            ExpressionBody resolvedBody = expressionBody.orElseThrow();
            expressions.add(new TemplateExpression(resolvedBody.content(), current == '*'));
            index = resolvedBody.nextIndex();
        }
        return expressions;
    }

    private Optional<ExpressionBody> readExpressionBody(String source, int bodyStart) {
        int depthBrace = 1;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;
        for (int index = bodyStart; index < source.length(); index++) {
            char current = source.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (current == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (current == '{') {
                depthBrace++;
            } else if (current == '}') {
                depthBrace--;
                if (depthBrace == 0) {
                    return Optional.of(new ExpressionBody(source.substring(bodyStart, index), index + 1));
                }
            }
        }
        return Optional.empty();
    }

    private List<List<String>> extractModelPaths(String expression, Set<String> excludedIdentifiers) {
        return TemplateExpressionPathExtractor.modelPaths(expression, excludedIdentifiers);
    }

    private List<List<String>> extractModelPaths(
        String expression,
        Set<String> excludedIdentifiers,
        List<List<String>> noArgMethodPaths
    ) {
        return TemplateExpressionPathExtractor.modelPaths(expression, excludedIdentifiers, noArgMethodPaths);
    }

    private Map<String, ModelPath> extractLoopVariablePaths(
        StructuredTemplateParser.ParsedTemplate template,
        Set<String> excludedIdentifiers
    ) {
        Map<String, ModelPath> loopVariables = new LinkedHashMap<>();
        Map<Integer, StructuredTemplateParser.TemplateElement> elementsByIndex = template.elements().stream()
            .collect(Collectors.toMap(StructuredTemplateParser.TemplateElement::index, element -> element));
        for (StructuredTemplateParser.TemplateElement element : template.elements()) {
            Set<String> ancestorLoopStatusAliases = activeLoopStatusAliases(element.parentIndex(), elementsByIndex);
            SelectionScope activeSelectionRoot = activeSelectionRoot(element, elementsByIndex, excludedIdentifiers);
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                if (!attribute.hasValue() || !isLoopDeclarationAttribute(attribute)) {
                    continue;
                }
                String raw = attribute.value();
                if (raw.isBlank()) {
                    continue;
                }
                int separator = raw.indexOf(':');
                if (separator <= 0 || separator >= raw.length() - 1) {
                    continue;
                }
                String variablePart = raw.substring(0, separator).trim();
                String iterablePart = raw.substring(separator + 1).trim();
                LoopVariables parsedLoopVariables = extractLoopVariables(variablePart);
                if (parsedLoopVariables.itemAliases().isEmpty()) {
                    continue;
                }
                String iterableExpression = iterablePart;
                boolean selectionExpression = iterableExpression.startsWith("*{") && iterableExpression.endsWith("}");
                if (iterableExpression.startsWith("${") && iterableExpression.endsWith("}")) {
                    iterableExpression = iterableExpression.substring(2, iterableExpression.length() - 1);
                } else if (selectionExpression) {
                    iterableExpression = iterableExpression.substring(2, iterableExpression.length() - 1);
                }
                if (selectionExpression && activeSelectionRoot.excluded()) {
                    continue;
                }
                List<List<String>> iterablePaths = extractModelPaths(iterableExpression, Set.of());
                for (List<String> iterablePath : iterablePaths) {
                    if (iterablePath.isEmpty()) {
                        continue;
                    }
                    if (selectionExpression && activeSelectionRoot.root().isPresent()) {
                        iterablePath = selectedPath(activeSelectionRoot.root().orElseThrow(), iterablePath);
                    }
                    ModelPath loopPath = ModelPath.of(iterablePath);
                    if (isLoopStatusPath(loopPath, ancestorLoopStatusAliases)) {
                        continue;
                    }
                    for (String alias : parsedLoopVariables.itemAliases()) {
                        loopVariables.putIfAbsent(alias, loopPath);
                    }
                    break;
                }
            }
        }
        return loopVariables;
    }

    private boolean isLoopStatusPath(ModelPath modelPath, Set<String> loopStatusAliases) {
        return loopStatusAliases.contains(modelPath.root());
    }

    private List<TemplateInference.ReferencedFragment> extractReferencedFragments(
        StructuredTemplateParser.ParsedTemplate template,
        Optional<String> currentTemplatePath
    ) {
        List<TemplateInference.ReferencedFragment> referencedFragments = new ArrayList<>();
        for (String raw : fragmentInsertionAttributeValues(template)) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            parseFragmentExpression(raw, currentTemplatePath)
                .map(expression -> new TemplateInference.ReferencedFragment(
                    expression.templatePath(),
                    expression.fragmentName(),
                    expression.arguments(),
                    expression.hasArgumentList(),
                    requiresChildModelRecursion(expression)
                ))
                .ifPresent(referencedFragments::add);
        }
        return referencedFragments;
    }

    private Map<String, Boolean> referencedTemplatePaths(List<TemplateInference.ReferencedFragment> referencedFragments) {
        Map<String, Boolean> referencedTemplatePaths = new LinkedHashMap<>();
        for (TemplateInference.ReferencedFragment reference : referencedFragments) {
            referencedTemplatePaths.merge(
                reference.templatePath(),
                reference.requiresChildModelRecursion(),
                (left, right) -> left || right
            );
        }
        return referencedTemplatePaths;
    }

    private Map<ModelPath, Object> extractSwitchCaseRepresentativeValues(
        StructuredTemplateParser.ParsedTemplate template,
        Set<String> excludedIdentifiers
    ) {
        Map<ModelPath, Object> representativeValues = new LinkedHashMap<>();
        Map<Integer, StructuredTemplateParser.TemplateElement> elementsByIndex = template.elements().stream()
            .collect(Collectors.toMap(StructuredTemplateParser.TemplateElement::index, element -> element));
        for (StructuredTemplateParser.TemplateElement element : template.elements()) {
            SelectionScope selectionRoot = activeSelectionRoot(element, elementsByIndex, excludedIdentifiers);
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                if (!attribute.hasValue() || !isSwitchAttribute(attribute)) {
                    continue;
                }
                Optional<ModelPath> switchPath = switchExpressionPath(attribute.value(), selectionRoot, excludedIdentifiers);
                if (switchPath.isEmpty() || representativeValues.containsKey(switchPath.orElseThrow())) {
                    continue;
                }
                firstRepresentativeCaseValue(element, template.subtree(element), elementsByIndex)
                    .ifPresent(value -> representativeValues.put(switchPath.orElseThrow(), value));
            }
        }
        return representativeValues;
    }

    private Optional<ModelPath> switchExpressionPath(
        String rawExpression,
        SelectionScope selectionRoot,
        Set<String> excludedIdentifiers
    ) {
        String expression = rawExpression.trim();
        boolean selectionExpression = expression.startsWith("*{") && expression.endsWith("}");
        if (expression.startsWith("${") && expression.endsWith("}")) {
            expression = expression.substring(2, expression.length() - 1);
        } else if (selectionExpression) {
            expression = expression.substring(2, expression.length() - 1);
        }
        if (selectionExpression && selectionRoot.excluded()) {
            return Optional.empty();
        }
        Optional<List<String>> directPath = TemplateExpressionPathExtractor.directModelPath(
            expression,
            excludedIdentifiers
        );
        if (directPath.isEmpty() || directPath.orElseThrow().isEmpty()) {
            return Optional.empty();
        }
        List<String> path = directPath.orElseThrow();
        if (selectionExpression && selectionRoot.root().isPresent()) {
            path = selectedPath(selectionRoot.root().orElseThrow(), path);
        }
        return Optional.of(ModelPath.of(path));
    }

    private Optional<Object> firstRepresentativeCaseValue(
        StructuredTemplateParser.TemplateElement switchElement,
        List<StructuredTemplateParser.TemplateElement> subtree,
        Map<Integer, StructuredTemplateParser.TemplateElement> elementsByIndex
    ) {
        for (StructuredTemplateParser.TemplateElement element : subtree) {
            if (!belongsToSwitch(element, switchElement, elementsByIndex)) {
                continue;
            }
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                if (!attribute.hasValue() || !isCaseAttribute(attribute)) {
                    continue;
                }
                Optional<Object> value = StaticLiteralValueParser.parse(attribute.value());
                if (value.isPresent()) {
                    return value;
                }
            }
        }
        return Optional.empty();
    }

    private boolean belongsToSwitch(
        StructuredTemplateParser.TemplateElement candidate,
        StructuredTemplateParser.TemplateElement switchElement,
        Map<Integer, StructuredTemplateParser.TemplateElement> elementsByIndex
    ) {
        if (candidate.index() == switchElement.index()) {
            return true;
        }
        StructuredTemplateParser.TemplateElement current = elementsByIndex.get(candidate.parentIndex());
        while (current != null) {
            if (hasSwitchAttribute(current)) {
                return current.index() == switchElement.index();
            }
            current = elementsByIndex.get(current.parentIndex());
        }
        return false;
    }

    private Optional<FragmentExpression> parseFragmentExpression(String raw, Optional<String> currentTemplatePath) {
        if (currentTemplatePath.isPresent()) {
            return fragmentExpressionParser.parse(raw, currentTemplatePath.orElseThrow());
        }
        return fragmentExpressionParser.parse(raw);
    }

    private List<String> fragmentInsertionAttributeValues(StructuredTemplateParser.ParsedTemplate template) {
        List<String> values = new ArrayList<>();
        for (StructuredTemplateParser.TemplateElement element : template.elements()) {
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                if (attribute.hasValue() && FragmentReferenceAttributes.isInsertionAttribute(attribute.name())) {
                    values.add(attribute.value());
                }
            }
        }
        return values;
    }

    private boolean requiresChildModelRecursion(FragmentExpression expression) {
        if (expression.arguments().isEmpty()) {
            return !expression.hasArgumentList();
        }
        for (String argument : expression.arguments()) {
            if (argument.isBlank()) {
                continue;
            }
            String value = argument;
            int assignIndex = argument.indexOf('=');
            if (assignIndex >= 0 && assignIndex < argument.length() - 1) {
                value = argument.substring(assignIndex + 1).trim();
            }
            if (!isLiteralExpression(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLiteralExpression(String value) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return true;
        }
        if (normalized.startsWith("'") && normalized.endsWith("'") && normalized.length() >= 2) {
            return true;
        }
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            return true;
        }
        if (normalized.equals("true") || normalized.equals("false") || normalized.equals("null")) {
            return true;
        }
        if (normalized.matches("[-+]?\\d+(\\.\\d+)?")) {
            return true;
        }
        return false;
    }

    private Set<String> extractLocalVariablesFromThWith(StructuredTemplateParser.ParsedTemplate template) {
        Set<String> localVariables = new HashSet<>();
        for (String raw : thymeleafAttributeValues(template, Set.of("th:with", "data-th-with"))) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            for (String assignment : splitTopLevel(raw, ',')) {
                int equalIndex = assignment.indexOf('=');
                if (equalIndex <= 0) {
                    continue;
                }
                String candidate = assignment.substring(0, equalIndex).trim();
                if (isValidIdentifier(candidate)) {
                    localVariables.add(candidate);
                }
            }
        }
        return localVariables;
    }

    private List<ExpressionSource> expressionSources(
        StructuredTemplateParser.ParsedTemplate template,
        Set<String> excludedIdentifiers
    ) {
        List<ExpressionSource> sources = new ArrayList<>();
        Map<Integer, StructuredTemplateParser.TemplateElement> elementsByIndex = template.elements().stream()
            .collect(Collectors.toMap(StructuredTemplateParser.TemplateElement::index, element -> element));
        for (StructuredTemplateParser.TemplateElement element : template.elements()) {
            Set<String> activeLoopStatusAliases = activeLoopStatusAliases(element, elementsByIndex);
            Set<String> ancestorLoopStatusAliases = activeLoopStatusAliases(element.parentIndex(), elementsByIndex);
            SelectionScope activeSelectionRoot = activeSelectionRoot(element, elementsByIndex, excludedIdentifiers);
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                if (attribute.hasValue()) {
                    Set<String> loopStatusAliases = isLoopDeclarationAttribute(attribute)
                        ? ancestorLoopStatusAliases
                        : activeLoopStatusAliases;
                    SelectionScope selectionRoot = isSelectionRootAttribute(attribute)
                        ? activeSelectionRoot(element.parentIndex(), elementsByIndex, excludedIdentifiers)
                        : activeSelectionRoot;
                    sources.add(new ExpressionSource(
                        attribute.value(),
                        loopStatusAliases,
                        selectionRoot.root(),
                        selectionRoot.excluded()
                    ));
                }
            }
        }
        for (StructuredTemplateParser.TemplateText text : template.textNodes()) {
            if (!text.content().isBlank()) {
                sources.add(new ExpressionSource(
                    text.content(),
                    activeLoopStatusAliases(text.parentIndex(), elementsByIndex),
                    activeSelectionRoot(text.parentIndex(), elementsByIndex, excludedIdentifiers).root(),
                    activeSelectionRoot(text.parentIndex(), elementsByIndex, excludedIdentifiers).excluded()
                ));
            }
        }
        return sources;
    }

    private Set<String> activeLoopStatusAliases(
        StructuredTemplateParser.TemplateElement element,
        Map<Integer, StructuredTemplateParser.TemplateElement> elementsByIndex
    ) {
        Set<String> aliases = new HashSet<>();
        StructuredTemplateParser.TemplateElement current = element;
        while (current != null) {
            aliases.addAll(loopStatusAliases(current));
            current = elementsByIndex.get(current.parentIndex());
        }
        return aliases;
    }

    private boolean isLoopDeclarationAttribute(StructuredTemplateParser.TemplateAttribute attribute) {
        String normalizedName = attribute.name().toLowerCase(java.util.Locale.ROOT);
        return normalizedName.equals("th:each") || normalizedName.equals("data-th-each");
    }

    private boolean isSelectionRootAttribute(StructuredTemplateParser.TemplateAttribute attribute) {
        String normalizedName = attribute.name().toLowerCase(java.util.Locale.ROOT);
        return normalizedName.equals("th:object") || normalizedName.equals("data-th-object");
    }

    private boolean isSwitchAttribute(StructuredTemplateParser.TemplateAttribute attribute) {
        String normalizedName = attribute.name().toLowerCase(java.util.Locale.ROOT);
        return normalizedName.equals("th:switch") || normalizedName.equals("data-th-switch");
    }

    private boolean hasSwitchAttribute(StructuredTemplateParser.TemplateElement element) {
        return element.attributes().stream().anyMatch(attribute -> attribute.hasValue() && isSwitchAttribute(attribute));
    }

    private boolean isCaseAttribute(StructuredTemplateParser.TemplateAttribute attribute) {
        String normalizedName = attribute.name().toLowerCase(java.util.Locale.ROOT);
        return normalizedName.equals("th:case") || normalizedName.equals("data-th-case");
    }

    private SelectionScope activeSelectionRoot(
        StructuredTemplateParser.TemplateElement element,
        Map<Integer, StructuredTemplateParser.TemplateElement> elementsByIndex,
        Set<String> excludedIdentifiers
    ) {
        StructuredTemplateParser.TemplateElement current = element;
        while (current != null) {
            SelectionScope root = selectionRoot(current, excludedIdentifiers);
            if (root.root().isPresent() || root.excluded()) {
                return root;
            }
            current = elementsByIndex.get(current.parentIndex());
        }
        return SelectionScope.none();
    }

    private SelectionScope activeSelectionRoot(
        int parentIndex,
        Map<Integer, StructuredTemplateParser.TemplateElement> elementsByIndex,
        Set<String> excludedIdentifiers
    ) {
        StructuredTemplateParser.TemplateElement parent = elementsByIndex.get(parentIndex);
        if (parent == null) {
            return SelectionScope.none();
        }
        return activeSelectionRoot(parent, elementsByIndex, excludedIdentifiers);
    }

    private SelectionScope selectionRoot(
        StructuredTemplateParser.TemplateElement element,
        Set<String> excludedIdentifiers
    ) {
        for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
            if (!attribute.hasValue() || !isSelectionRootAttribute(attribute)) {
                continue;
            }
            String expression = attribute.value().trim();
            if (expression.startsWith("${") && expression.endsWith("}")) {
                expression = expression.substring(2, expression.length() - 1);
            }
            List<List<String>> allPaths = extractModelPaths(expression, Set.of());
            if (!allPaths.isEmpty() && !allPaths.getFirst().isEmpty()) {
                ModelPath rawRoot = ModelPath.of(allPaths.getFirst());
                if (excludedIdentifiers.contains(rawRoot.root())) {
                    return SelectionScope.excludedScope();
                }
            }
            List<List<String>> paths = extractModelPaths(expression, excludedIdentifiers);
            if (!paths.isEmpty() && !paths.getFirst().isEmpty()) {
                return SelectionScope.of(ModelPath.of(paths.getFirst()));
            }
        }
        return SelectionScope.none();
    }

    private Set<String> activeLoopStatusAliases(
        int parentIndex,
        Map<Integer, StructuredTemplateParser.TemplateElement> elementsByIndex
    ) {
        StructuredTemplateParser.TemplateElement parent = elementsByIndex.get(parentIndex);
        if (parent == null) {
            return Set.of();
        }
        return activeLoopStatusAliases(parent, elementsByIndex);
    }

    private Set<String> loopStatusAliases(StructuredTemplateParser.TemplateElement element) {
        Set<String> aliases = new HashSet<>();
        for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
            if (!attribute.hasValue()) {
                continue;
            }
            String normalizedName = attribute.name().toLowerCase(java.util.Locale.ROOT);
            if (!normalizedName.equals("th:each") && !normalizedName.equals("data-th-each")) {
                continue;
            }
            int separator = attribute.value().indexOf(':');
            if (separator <= 0) {
                continue;
            }
            aliases.addAll(extractLoopVariables(attribute.value().substring(0, separator).trim()).statusAliases());
        }
        return aliases;
    }

    private List<String> thymeleafAttributeValues(
        StructuredTemplateParser.ParsedTemplate template,
        Set<String> attributeNames
    ) {
        List<String> values = new ArrayList<>();
        for (StructuredTemplateParser.TemplateElement element : template.elements()) {
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                if (!attribute.hasValue()) {
                    continue;
                }
                String normalizedName = attribute.name().toLowerCase(java.util.Locale.ROOT);
                if (attributeNames.contains(normalizedName)) {
                    values.add(attribute.value());
                }
            }
        }
        return values;
    }

    private LoopVariables extractLoopVariables(String variablePart) {
        String normalized = variablePart.trim();
        if (normalized.isEmpty()) {
            return new LoopVariables(List.of(), List.of());
        }
        boolean tupleStyle = normalized.startsWith("(") && normalized.endsWith(")") && normalized.length() > 2;
        if (tupleStyle) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        List<String> aliases = new ArrayList<>();
        for (String token : splitTopLevel(normalized, ',')) {
            String alias = token.trim();
            if (alias.isEmpty()) {
                continue;
            }
            int eqIndex = alias.indexOf('=');
            if (eqIndex > 0) {
                alias = alias.substring(0, eqIndex).trim();
            }
            if (isValidIdentifier(alias)) {
                aliases.add(alias);
            }
        }
        if (tupleStyle || aliases.size() <= 1) {
            List<String> statusAliases = aliases.size() == 1 && !tupleStyle
                ? List.of(aliases.getFirst() + "Stat")
                : List.of();
            return new LoopVariables(aliases, statusAliases);
        }
        return new LoopVariables(List.of(aliases.getFirst()), aliases.subList(1, aliases.size()));
    }

    private record LoopVariables(List<String> itemAliases, List<String> statusAliases) {
        private LoopVariables {
            itemAliases = List.copyOf(itemAliases);
            statusAliases = List.copyOf(statusAliases);
        }
    }

    private record TemplateExpression(String content, boolean selectionExpression) {
    }

    private record SelectionScope(Optional<ModelPath> root, boolean excluded) {
        static SelectionScope none() {
            return new SelectionScope(Optional.empty(), false);
        }

        static SelectionScope of(ModelPath root) {
            return new SelectionScope(Optional.of(root), false);
        }

        static SelectionScope excludedScope() {
            return new SelectionScope(Optional.empty(), true);
        }
    }

    private record ExpressionBody(String content, int nextIndex) {
    }

    private record ExpressionSource(
        String value,
        Set<String> loopStatusAliases,
        Optional<ModelPath> selectionRoot,
        boolean selectionRootExcluded
    ) {
        private ExpressionSource {
            loopStatusAliases = Set.copyOf(loopStatusAliases);
            selectionRoot = selectionRoot == null ? Optional.empty() : selectionRoot;
        }
    }

    private boolean isValidIdentifier(String candidate) {
        if (candidate.isEmpty() || !isIdentifierStart(candidate.charAt(0))) {
            return false;
        }
        for (int i = 1; i < candidate.length(); i++) {
            if (!isIdentifierPart(candidate.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private List<String> splitTopLevel(String value, char separator) {
        List<String> segments = new ArrayList<>();
        int depthParen = 0;
        int depthBracket = 0;
        int depthBrace = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int segmentStart = 0;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            char previous = i > 0 ? value.charAt(i - 1) : '\0';
            if (c == '\'' && !inDoubleQuote && previous != '\\') {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (c == '"' && !inSingleQuote && previous != '\\') {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (c == '(') {
                depthParen++;
            } else if (c == ')' && depthParen > 0) {
                depthParen--;
            } else if (c == '[') {
                depthBracket++;
            } else if (c == ']' && depthBracket > 0) {
                depthBracket--;
            } else if (c == '{') {
                depthBrace++;
            } else if (c == '}' && depthBrace > 0) {
                depthBrace--;
            } else if (c == separator && depthParen == 0 && depthBracket == 0 && depthBrace == 0) {
                segments.add(value.substring(segmentStart, i).trim());
                segmentStart = i + 1;
            }
        }
        if (segmentStart <= value.length()) {
            segments.add(value.substring(segmentStart).trim());
        }
        return segments;
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }
}
