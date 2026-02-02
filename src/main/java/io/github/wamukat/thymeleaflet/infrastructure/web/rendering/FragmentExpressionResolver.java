package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.standard.expression.Fragment;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.springframework.stereotype.Component;

@Component
public class FragmentExpressionResolver {

    public Object resolve(Object fragmentExpression, Object context) {
        if (fragmentExpression == null) {
            return null;
        }
        if (fragmentExpression instanceof Fragment) {
            return fragmentExpression;
        }
        if (!(fragmentExpression instanceof String)) {
            return fragmentExpression;
        }
        if (!(context instanceof IExpressionContext expressionContext)) {
            return fragmentExpression;
        }
        String expressionString = fragmentExpression.toString().trim();
        if (!expressionString.startsWith("~{")) {
            return fragmentExpression;
        }
        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(expressionContext.getConfiguration());
        IStandardExpression expression = parser.parseExpression(expressionContext, expressionString);
        return expression.execute(expressionContext);
    }
}
