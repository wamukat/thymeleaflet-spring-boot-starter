package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IPreProcessorDialect;
import org.thymeleaf.preprocessor.IPreProcessor;
import org.thymeleaf.preprocessor.PreProcessor;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Set;

public final class NoArgFragmentReferencePreProcessorDialect
    extends AbstractDialect
    implements IPreProcessorDialect {

    public NoArgFragmentReferencePreProcessorDialect() {
        super("thymeleaflet-no-arg-fragment-reference");
    }

    @Override
    public int getDialectPreProcessorPrecedence() {
        return 1000;
    }

    @Override
    public Set<IPreProcessor> getPreProcessors() {
        return Set.of(new PreProcessor(
            TemplateMode.HTML,
            NoArgFragmentReferencePreProcessor.class,
            1000
        ));
    }
}
