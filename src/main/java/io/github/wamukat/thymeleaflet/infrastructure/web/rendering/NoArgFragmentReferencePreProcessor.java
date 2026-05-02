package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import io.github.wamukat.thymeleaflet.domain.service.FragmentReferenceAttributes;
import io.github.wamukat.thymeleaflet.domain.service.TopLevelSyntaxScanner;
import org.thymeleaf.engine.AbstractTemplateHandler;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IStandaloneElementTag;

import java.util.Optional;

public final class NoArgFragmentReferencePreProcessor extends AbstractTemplateHandler {

    private static final TopLevelSyntaxScanner TOP_LEVEL_SYNTAX_SCANNER = new TopLevelSyntaxScanner();

    @Override
    public void handleOpenElement(IOpenElementTag tag) {
        getNext().handleOpenElement(normalizeFragmentInsertionAttributes(tag));
    }

    @Override
    public void handleStandaloneElement(IStandaloneElementTag tag) {
        getNext().handleStandaloneElement(normalizeFragmentInsertionAttributes(tag));
    }

    private <T extends IProcessableElementTag> T normalizeFragmentInsertionAttributes(T tag) {
        T updated = tag;
        for (IAttribute attribute : tag.getAllAttributes()) {
            String value = attribute.getValue();
            if (!isFragmentInsertionAttribute(attribute) || value == null) {
                continue;
            }

            String normalized = normalizeNoArgFragmentSelector(value);
            if (normalized.equals(value)) {
                continue;
            }

            updated = getContext().getModelFactory().replaceAttribute(
                updated,
                attribute.getAttributeDefinition().getAttributeName(),
                attribute.getAttributeCompleteName(),
                normalized,
                attribute.getValueQuotes()
            );
        }
        return updated;
    }

    static String normalizeNoArgFragmentSelector(String value) {
        return FragmentExpressionValue.from(value)
            .flatMap(FragmentExpressionValue::normalizeNoArgSelector)
            .orElse(value);
    }

    private static boolean isFragmentInsertionAttribute(IAttribute attribute) {
        return FragmentReferenceAttributes.isInsertionAttribute(attribute.getAttributeCompleteName());
    }

    private record FragmentExpressionValue(String value, int bodyStart, int bodyEnd) {

        static Optional<FragmentExpressionValue> from(String value) {
            int expressionStart = leadingWhitespaceLength(value);
            int expressionEnd = trailingWhitespaceStart(value);
            if (expressionStart >= expressionEnd) {
                return Optional.empty();
            }
            String expression = value.substring(expressionStart, expressionEnd);
            if (expression.startsWith("${") || expression.startsWith("*{") || expression.startsWith("#{")) {
                return Optional.empty();
            }
            if (expression.startsWith("~{") && expression.endsWith("}")) {
                return Optional.of(new FragmentExpressionValue(value, expressionStart + 2, expressionEnd - 1));
            }
            return Optional.of(new FragmentExpressionValue(value, expressionStart, expressionEnd));
        }

        Optional<String> normalizeNoArgSelector() {
            return findTopLevelFragmentSeparator()
                .flatMap(separatorIndex -> normalizeSelector(separatorIndex + 2));
        }

        private Optional<Integer> findTopLevelFragmentSeparator() {
            var separatorIndex = TOP_LEVEL_SYNTAX_SCANNER.findFirst(value, "::", bodyStart, bodyEnd);
            if (separatorIndex.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(separatorIndex.getAsInt());
        }

        private Optional<String> normalizeSelector(int selectorStart) {
            String selector = value.substring(selectorStart, bodyEnd);
            int leadingLength = leadingWhitespaceLength(selector);
            int trailingStart = trailingWhitespaceStart(selector);
            String leading = selector.substring(0, leadingLength);
            String trailing = selector.substring(trailingStart);
            String trimmedSelector = selector.substring(leadingLength, trailingStart);
            return SelectorToken.from(trimmedSelector)
                .flatMap(SelectorToken::withoutEmptyArgumentList)
                .map(normalizedSelector ->
                    value.substring(0, selectorStart) + leading + normalizedSelector + trailing + value.substring(bodyEnd)
                );
        }

        private static int leadingWhitespaceLength(String value) {
            int index = 0;
            while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
                index++;
            }
            return index;
        }

        private static int trailingWhitespaceStart(String value) {
            int index = value.length();
            while (index > 0 && Character.isWhitespace(value.charAt(index - 1))) {
                index--;
            }
            return index;
        }
    }

    private record SelectorToken(String value, String quote, String content) {

        static Optional<SelectorToken> from(String value) {
            if (value.isBlank()) {
                return Optional.empty();
            }
            if (isQuoted(value)) {
                return Optional.of(new SelectorToken(value, value.substring(0, 1), value.substring(1, value.length() - 1)));
            }
            return Optional.of(new SelectorToken(value, "", value));
        }

        Optional<String> withoutEmptyArgumentList() {
            int openParen = findTopLevelOpenParen(content);
            if (openParen < 0) {
                return Optional.empty();
            }
            String fragmentName = content.substring(0, openParen).trim();
            if (!isValidFragmentName(fragmentName)) {
                return Optional.empty();
            }
            int closeParen = lastNonWhitespaceIndex(content);
            if (closeParen <= openParen || content.charAt(closeParen) != ')') {
                return Optional.empty();
            }
            if (!content.substring(openParen + 1, closeParen).isBlank()) {
                return Optional.empty();
            }
            return Optional.of(quote + fragmentName + quote);
        }

        private static boolean isQuoted(String value) {
            return value.length() >= 2
                && ((value.startsWith("'") && value.endsWith("'"))
                || (value.startsWith("\"") && value.endsWith("\"")));
        }

        private static int findTopLevelOpenParen(String value) {
            return TOP_LEVEL_SYNTAX_SCANNER.findFirst(value, "(").orElse(-1);
        }

        private static int lastNonWhitespaceIndex(String value) {
            int index = value.length() - 1;
            while (index >= 0 && Character.isWhitespace(value.charAt(index))) {
                index--;
            }
            return index;
        }

        private static boolean isValidFragmentName(String value) {
            if (value.isEmpty()) {
                return false;
            }
            for (int index = 0; index < value.length(); index++) {
                char current = value.charAt(index);
                if (!Character.isLetterOrDigit(current) && current != '_' && current != '-') {
                    return false;
                }
            }
            return true;
        }
    }

}
