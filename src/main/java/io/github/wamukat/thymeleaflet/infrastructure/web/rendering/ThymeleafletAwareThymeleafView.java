package io.github.wamukat.thymeleaflet.infrastructure.web.rendering;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.MethodResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateView;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.WebExpressionContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.spring6.ISpringTemplateEngine;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.spring6.naming.SpringContextVariableNames;
import org.thymeleaf.spring6.util.SpringContentTypeUtils;
import org.thymeleaf.spring6.util.SpringRequestUtils;
import org.thymeleaf.spring6.view.AbstractThymeleafView;
import org.thymeleaf.spring6.view.ThymeleafView;
import org.thymeleaf.standard.expression.FragmentExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.util.FastStringWriter;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * /render 時にのみ no-arg メソッド互換 resolver を評価コンテキストへ追加する ThymeleafView 拡張。
 */
public class ThymeleafletAwareThymeleafView extends ThymeleafView {

    private static final @Nullable String PATH_VARIABLES_SELECTOR = computePathVariablesSelector();
    private static final String RENDER_SUFFIX = "/render";
    private static final String THYMELEAFLET_PREFIX = "/thymeleaflet/";

    @Override
    protected void renderFragment(
        Set<String> markupSelectorsToRender,
        Map<String, ?> model,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws Exception {
        ServletContext servletContext = getServletContext();
        IWebExchange webExchange =
            JakartaServletWebApplication.buildApplication(servletContext).buildExchange(request, response);

        String viewTemplateName = getTemplateName();
        ISpringTemplateEngine viewTemplateEngine = getTemplateEngine();

        if (viewTemplateName == null) {
            throw new IllegalArgumentException("Property 'templateName' is required");
        }
        if (getLocale() == null) {
            throw new IllegalArgumentException("Property 'locale' is required");
        }
        if (viewTemplateEngine == null) {
            throw new IllegalArgumentException("Property 'templateEngine' is required");
        }

        Map<String, Object> mergedModel = new HashMap<>(30);
        Map<String, Object> templateStaticVariables = getStaticVariables();
        if (templateStaticVariables != null) {
            mergedModel.putAll(templateStaticVariables);
        }
        if (PATH_VARIABLES_SELECTOR != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(PATH_VARIABLES_SELECTOR);
            if (pathVars != null) {
                mergedModel.putAll(pathVars);
            }
        }
        if (model != null) {
            mergedModel.putAll(model);
        }

        ApplicationContext applicationContext = getApplicationContext();
        RequestContext requestContext = new RequestContext(request, response, getServletContext(), mergedModel);
        SpringWebMvcThymeleafRequestContext thymeleafRequestContext =
            new SpringWebMvcThymeleafRequestContext(requestContext, request);

        addRequestContextAsVariable(
            mergedModel, SpringContextVariableNames.SPRING_REQUEST_CONTEXT, requestContext
        );
        addRequestContextAsVariable(
            mergedModel, AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, requestContext
        );
        mergedModel.put(SpringContextVariableNames.THYMELEAF_REQUEST_CONTEXT, thymeleafRequestContext);

        ConversionService conversionService =
            (ConversionService) request.getAttribute(ConversionService.class.getName());
        ThymeleafEvaluationContext evaluationContext =
            new ThymeleafEvaluationContext(applicationContext, conversionService);
        if (isThymeleafletRenderRequest(request)) {
            List<MethodResolver> methodResolvers = new ArrayList<>();
            methodResolvers.add(new MapNoArgMethodResolver(applicationContext));
            methodResolvers.addAll(evaluationContext.getMethodResolvers());
            evaluationContext.setMethodResolvers(methodResolvers);
        }
        mergedModel.put(
            ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
            evaluationContext
        );

        IEngineConfiguration configuration = viewTemplateEngine.getConfiguration();
        WebExpressionContext context =
            new WebExpressionContext(configuration, webExchange, getLocale(), mergedModel);

        String templateName;
        Set<String> markupSelectors;
        if (!viewTemplateName.contains("::")) {
            templateName = viewTemplateName;
            markupSelectors = null;
        } else {
            SpringRequestUtils.checkViewNameNotInRequest(viewTemplateName, webExchange.getRequest());
            IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);

            FragmentExpression fragmentExpression;
            try {
                fragmentExpression = (FragmentExpression) parser.parseExpression(
                    context,
                    "~{" + viewTemplateName + "}"
                );
            } catch (TemplateProcessingException templateProcessingException) {
                throw new IllegalArgumentException(
                    "Invalid template name specification: '" + viewTemplateName + "'"
                );
            }

            FragmentExpression.ExecutedFragmentExpression fragment =
                FragmentExpression.createExecutedFragmentExpression(context, fragmentExpression);

            templateName = FragmentExpression.resolveTemplateName(fragment);
            markupSelectors = FragmentExpression.resolveFragments(fragment);
            Map<String, Object> nameFragmentParameters = fragment.getFragmentParameters();

            if (nameFragmentParameters != null) {
                if (fragment.hasSyntheticParameters()) {
                    throw new IllegalArgumentException(
                        "Parameters in a view specification must be named (non-synthetic): '"
                            + viewTemplateName
                            + "'"
                    );
                }
                context.setVariables(nameFragmentParameters);
            }
        }

        String templateContentType = getContentType();
        String templateCharacterEncoding = getCharacterEncoding();

        Set<String> processMarkupSelectors;
        if (markupSelectors != null && !markupSelectors.isEmpty()) {
            if (markupSelectorsToRender != null && !markupSelectorsToRender.isEmpty()) {
                throw new IllegalArgumentException(
                    "A markup selector has been specified (" + Arrays.asList(markupSelectors)
                        + ") for a view that was already being executed as a fragment ("
                        + Arrays.asList(markupSelectorsToRender) + "). Only one fragment selection is allowed."
                );
            }
            processMarkupSelectors = markupSelectors;
        } else if (markupSelectorsToRender != null && !markupSelectorsToRender.isEmpty()) {
            processMarkupSelectors = markupSelectorsToRender;
        } else {
            processMarkupSelectors = null;
        }

        response.setLocale(getLocale());

        if (!getForceContentType()) {
            String computedContentType = SpringContentTypeUtils.computeViewContentType(
                webExchange,
                (templateContentType != null ? templateContentType : DEFAULT_CONTENT_TYPE),
                (templateCharacterEncoding != null ? Charset.forName(templateCharacterEncoding) : null)
            );
            response.setContentType(computedContentType);
        } else {
            if (templateContentType != null) {
                response.setContentType(templateContentType);
            } else {
                response.setContentType(DEFAULT_CONTENT_TYPE);
            }
            if (templateCharacterEncoding != null) {
                response.setCharacterEncoding(templateCharacterEncoding);
            }
        }

        boolean producePartialOutputWhileProcessing = getProducePartialOutputWhileProcessing();
        Writer templateWriter =
            producePartialOutputWhileProcessing ? response.getWriter() : new FastStringWriter(1024);

        viewTemplateEngine.process(templateName, processMarkupSelectors, context, templateWriter);

        if (!producePartialOutputWhileProcessing) {
            response.getWriter().write(templateWriter.toString());
            response.getWriter().flush();
        }
    }

    private static @Nullable String computePathVariablesSelector() {
        try {
            Field pathVariablesField = View.class.getDeclaredField("PATH_VARIABLES");
            return (String) pathVariablesField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }

    private boolean isThymeleafletRenderRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null) {
            return false;
        }
        return requestUri.startsWith(THYMELEAFLET_PREFIX) && requestUri.endsWith(RENDER_SUFFIX);
    }

}
