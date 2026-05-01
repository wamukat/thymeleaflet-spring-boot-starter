package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import org.thymeleaf.engine.AbstractTemplateHandler;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IStandaloneElementTag;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NoArgFragmentReferencePreProcessor extends AbstractTemplateHandler {

    private static final Pattern NO_ARG_FRAGMENT_SELECTOR =
        Pattern.compile("(::\\s*)('?)([A-Za-z0-9_-]+)\\s*\\(\\s*\\)('?)");

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
        Matcher matcher = NO_ARG_FRAGMENT_SELECTOR.matcher(value);
        return matcher.replaceAll("$1$2$3$4");
    }

    private static boolean isFragmentInsertionAttribute(IAttribute attribute) {
        String attributeName = attribute.getAttributeCompleteName().toLowerCase(Locale.ROOT);
        return attributeName.equals("th:replace")
            || attributeName.equals("th:insert")
            || attributeName.equals("data-th-replace")
            || attributeName.equals("data-th-insert");
    }
}
