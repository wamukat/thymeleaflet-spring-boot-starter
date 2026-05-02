package io.github.wamukat.thymeleaflet.domain.service;

import org.attoparser.AbstractMarkupHandler;
import org.attoparser.MarkupParser;
import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Internal parser facade for template-level extraction that should match Thymeleaf's HTML parser.
 */
public class StructuredTemplateParser {

    public ParsedTemplate parse(String html) {
        Objects.requireNonNull(html, "html cannot be null");
        CollectingMarkupHandler handler = new CollectingMarkupHandler();
        try {
            new MarkupParser(ParseConfiguration.htmlConfiguration()).parse(html, handler);
        } catch (ParseException parseException) {
            throw new IllegalArgumentException("Failed to parse template markup", parseException);
        }
        return handler.toParsedTemplate();
    }

    public TemplateParseResult parseWithDiagnostics(String html) {
        Objects.requireNonNull(html, "html cannot be null");
        try {
            ParsedTemplate parsedTemplate = parse(html);
            return new TemplateParseResult(parsedTemplate, diagnosticsFor(parsedTemplate));
        } catch (IllegalArgumentException parseFailure) {
            return new TemplateParseResult(
                new ParsedTemplate(List.of(), List.of(), List.of()),
                List.of(ParserDiagnostic.warning("TEMPLATE_MARKUP_MALFORMED", diagnosticMessage(parseFailure)))
            );
        }
    }

    private String diagnosticMessage(Exception failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message;
    }

    private List<ParserDiagnostic> diagnosticsFor(ParsedTemplate parsedTemplate) {
        List<ParserDiagnostic> diagnostics = new ArrayList<>();
        for (TemplateElement element : parsedTemplate.elements()) {
            for (TemplateAttribute attribute : element.attributes()) {
                if (isFragmentReferenceAttribute(attribute) && isDynamicSyntax(attribute.value())) {
                    diagnostics.add(new ParserDiagnostic(
                        "TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED",
                        "Dynamic fragment reference was skipped in `" + attribute.name() + "`",
                        attribute.line(),
                        attribute.column()
                    ));
                }
            }
        }
        return diagnostics;
    }

    private boolean isFragmentReferenceAttribute(TemplateAttribute attribute) {
        return attribute.hasValue() && FragmentReferenceAttributes.isReferenceAttribute(attribute.name());
    }

    private boolean isDynamicSyntax(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("${") || trimmed.startsWith("*{") || trimmed.startsWith("#{");
    }

    public record TemplateParseResult(ParsedTemplate parsedTemplate, List<ParserDiagnostic> diagnostics) {
        public TemplateParseResult {
            parsedTemplate = Objects.requireNonNull(parsedTemplate, "parsedTemplate cannot be null");
            diagnostics = List.copyOf(diagnostics);
        }
    }

    public record ParsedTemplate(List<TemplateElement> elements, List<TemplateText> textNodes, List<TemplateComment> comments) {
        public ParsedTemplate {
            elements = List.copyOf(elements);
            textNodes = List.copyOf(textNodes);
            comments = List.copyOf(comments);
        }

        public List<TemplateElement> subtree(TemplateElement root) {
            Objects.requireNonNull(root, "root cannot be null");
            Map<Integer, TemplateElement> byIndex = elements.stream()
                .collect(Collectors.toUnmodifiableMap(TemplateElement::index, element -> element));
            return elements.stream()
                .filter(element -> element.index() == root.index() || isDescendantOf(element, root.index(), byIndex))
                .toList();
        }

        public List<TemplateElement> elementsMatchingSubtree(Predicate<TemplateElement> rootPredicate) {
            Objects.requireNonNull(rootPredicate, "rootPredicate cannot be null");
            return elements.stream()
                .filter(rootPredicate)
                .findFirst()
                .map(this::subtree)
                .orElse(elements);
        }

        private static boolean isDescendantOf(
            TemplateElement candidate,
            int rootIndex,
            Map<Integer, TemplateElement> byIndex
        ) {
            int parentIndex = candidate.parentIndex();
            while (parentIndex >= 0) {
                if (parentIndex == rootIndex) {
                    return true;
                }
                TemplateElement parent = byIndex.get(parentIndex);
                if (parent == null) {
                    return false;
                }
                parentIndex = parent.parentIndex();
            }
            return false;
        }
    }

    public record TemplateElement(
        String name,
        List<TemplateAttribute> attributes,
        int line,
        int column,
        int index,
        int parentIndex,
        int depth
    ) {
        public TemplateElement {
            name = name.trim();
            attributes = List.copyOf(attributes);
        }

        public TemplateElement(String name, List<TemplateAttribute> attributes, int line, int column) {
            this(name, attributes, line, column, -1, -1, 0);
        }

        public Optional<String> attributeValue(String attributeName) {
            Objects.requireNonNull(attributeName, "attributeName cannot be null");
            return attributes.stream()
                .filter(attribute -> attribute.name().equals(attributeName))
                .findFirst()
                .filter(TemplateAttribute::hasValue)
                .map(TemplateAttribute::value);
        }

        public List<TemplateAttribute> thymeleafAttributes() {
            return attributes.stream()
                .filter(TemplateAttribute::isThymeleafAttribute)
                .toList();
        }
    }

    public record TemplateAttribute(String name, String value, boolean hasValue, int line, int column) {
        public TemplateAttribute {
            name = name.trim();
        }

        public boolean isThymeleafAttribute() {
            String normalized = name.toLowerCase(Locale.ROOT);
            return normalized.startsWith("th:") || normalized.startsWith("data-th-");
        }
    }

    public record TemplateComment(String content, int line, int column) {
    }

    public record TemplateText(String content, int line, int column) {
    }

    private static final class CollectingMarkupHandler extends AbstractMarkupHandler {

        private final List<MutableTemplateElement> elements = new ArrayList<>();
        private final List<Integer> openElementStack = new ArrayList<>();
        private final List<TemplateText> textNodes = new ArrayList<>();
        private final List<TemplateComment> comments = new ArrayList<>();
        private Optional<MutableTemplateElement> currentElement = Optional.empty();

        @Override
        public void handleOpenElementStart(
            char[] buffer,
            int nameOffset,
            int nameLen,
            int line,
            int col
        ) {
            startElement(buffer, nameOffset, nameLen, line, col);
        }

        @Override
        public void handleStandaloneElementStart(
            char[] buffer,
            int nameOffset,
            int nameLen,
            boolean minimized,
            int line,
            int col
        ) {
            startElement(buffer, nameOffset, nameLen, line, col);
        }

        @Override
        public void handleOpenElementEnd(
            char[] buffer,
            int nameOffset,
            int nameLen,
            int line,
            int col
        ) {
            currentElement.ifPresent(element -> openElementStack.add(element.index));
            currentElement = Optional.empty();
        }

        @Override
        public void handleStandaloneElementEnd(
            char[] buffer,
            int nameOffset,
            int nameLen,
            boolean minimized,
            int line,
            int col
        ) {
            currentElement = Optional.empty();
        }

        @Override
        public void handleCloseElementStart(
            char[] buffer,
            int nameOffset,
            int nameLen,
            int line,
            int col
        ) {
            closeElement(buffer, nameOffset, nameLen);
        }

        @Override
        public void handleAutoCloseElementStart(
            char[] buffer,
            int nameOffset,
            int nameLen,
            int line,
            int col
        ) {
            closeElement(buffer, nameOffset, nameLen);
        }

        @Override
        public void handleAttribute(
            char[] buffer,
            int nameOffset,
            int nameLen,
            int nameLine,
            int nameCol,
            int operatorOffset,
            int operatorLen,
            int operatorLine,
            int operatorCol,
            int valueContentOffset,
            int valueContentLen,
            int valueOuterOffset,
            int valueOuterLen,
            int valueLine,
            int valueCol
        ) {
            if (currentElement.isEmpty()) {
                return;
            }
            String name = slice(buffer, nameOffset, nameLen);
            boolean hasValue = valueOuterOffset >= 0 && valueOuterLen >= 0;
            String value = hasValue ? slice(buffer, valueContentOffset, valueContentLen) : "";
            currentElement.orElseThrow().attributes.add(new TemplateAttribute(name, value, hasValue, nameLine, nameCol));
        }

        @Override
        public void handleComment(
            char[] buffer,
            int contentOffset,
            int contentLen,
            int outerOffset,
            int outerLen,
            int line,
            int col
        ) {
            comments.add(new TemplateComment(slice(buffer, contentOffset, contentLen), line, col));
        }

        @Override
        public void handleText(
            char[] buffer,
            int offset,
            int len,
            int line,
            int col
        ) {
            textNodes.add(new TemplateText(slice(buffer, offset, len), line, col));
        }

        private void startElement(char[] buffer, int nameOffset, int nameLen, int line, int col) {
            int index = elements.size();
            int parentIndex = openElementStack.isEmpty() ? -1 : openElementStack.get(openElementStack.size() - 1);
            MutableTemplateElement element = new MutableTemplateElement(
                slice(buffer, nameOffset, nameLen),
                line,
                col,
                index,
                parentIndex,
                openElementStack.size()
            );
            elements.add(element);
            currentElement = Optional.of(element);
        }

        private void closeElement(char[] buffer, int nameOffset, int nameLen) {
            String name = slice(buffer, nameOffset, nameLen);
            for (int index = openElementStack.size() - 1; index >= 0; index--) {
                MutableTemplateElement openElement = elements.get(openElementStack.get(index));
                openElementStack.remove(index);
                if (openElement.name.equalsIgnoreCase(name)) {
                    return;
                }
            }
        }

        private ParsedTemplate toParsedTemplate() {
            return new ParsedTemplate(
                elements.stream().map(MutableTemplateElement::toTemplateElement).toList(),
                textNodes,
                comments
            );
        }

        private static String slice(char[] buffer, int offset, int length) {
            if (offset < 0 || length <= 0) {
                return "";
            }
            return new String(buffer, offset, length);
        }
    }

    private static final class MutableTemplateElement {
        private final String name;
        private final int line;
        private final int column;
        private final int index;
        private final int parentIndex;
        private final int depth;
        private final List<TemplateAttribute> attributes = new ArrayList<>();

        private MutableTemplateElement(String name, int line, int column, int index, int parentIndex, int depth) {
            this.name = name;
            this.line = line;
            this.column = column;
            this.index = index;
            this.parentIndex = parentIndex;
            this.depth = depth;
        }

        private TemplateElement toTemplateElement() {
            return new TemplateElement(name, attributes, line, column, index, parentIndex, depth);
        }
    }
}
