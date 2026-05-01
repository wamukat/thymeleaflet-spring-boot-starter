package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.mock.env.MockEnvironment;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorybookAutoConfigurationTest {

    @Test
    void thymeleafletMessageSourcePostProcessorBeanFactoryMethod_shouldBeStatic() throws NoSuchMethodException {
        Method method = StorybookAutoConfiguration.class.getDeclaredMethod("thymeleafletMessageSourcePostProcessor");

        assertTrue(
            Modifier.isStatic(method.getModifiers()),
            "BeanPostProcessor factory method should be static to avoid BeanPostProcessorChecker warnings"
        );
    }

    @Test
    void autoConfigurationAfterName_shouldCoverBoot3AndBoot4WebMvcAutoConfiguration() {
        AutoConfiguration annotation = StorybookAutoConfiguration.class.getAnnotation(AutoConfiguration.class);
        assertNotNull(annotation, "StorybookAutoConfiguration should have @AutoConfiguration");

        assertTrue(
            Arrays.asList(annotation.afterName()).contains(
                "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration"
            ),
            "afterName should include Spring Boot 3 WebMvc auto-configuration"
        );
        assertTrue(
            Arrays.asList(annotation.afterName()).contains(
                "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration"
            ),
            "afterName should include Spring Boot 4 WebMvc auto-configuration"
        );
    }

    @Test
    void autoConfiguration_shouldBeDisableableWithThymeleafletEnabledFalse() {
        ConditionalOnProperty annotation = StorybookAutoConfiguration.class.getAnnotation(ConditionalOnProperty.class);

        assertNotNull(annotation, "StorybookAutoConfiguration should support thymeleaflet.enabled=false");
        assertEquals("thymeleaflet.enabled", annotation.name()[0]);
        assertEquals("true", annotation.havingValue());
        assertTrue(annotation.matchIfMissing());
    }

    @Test
    void thymeleafletObjectMapperBean_shouldBeConditionalOnMissingBean() throws NoSuchMethodException {
        Method method = StorybookAutoConfiguration.class.getDeclaredMethod("thymeleafletObjectMapper");
        ConditionalOnMissingBean annotation = method.getAnnotation(ConditionalOnMissingBean.class);

        assertNotNull(annotation, "thymeleafletObjectMapper should be conditional on missing bean");
        assertEquals(ObjectMapper.class, method.getReturnType(), "thymeleafletObjectMapper should return ObjectMapper");
        assertTrue(
            Arrays.asList(annotation.value()).contains(ObjectMapper.class),
            "thymeleafletObjectMapper should be conditional on ObjectMapper.class"
        );
    }

    @Test
    void thymeleafletClassLoaderTemplateResolverBean_shouldTargetOnlyThymeleafletTemplates() {
        StorybookAutoConfiguration configuration = new StorybookAutoConfiguration();

        ITemplateResolver templateResolver = configuration.thymeleafletClassLoaderTemplateResolver();

        assertTrue(
            templateResolver instanceof ClassLoaderTemplateResolver,
            "Thymeleaflet templates should be resolved from the starter JAR class loader"
        );
        ClassLoaderTemplateResolver resolver = (ClassLoaderTemplateResolver) templateResolver;
        assertEquals("templates/", resolver.getPrefix());
        assertEquals(".html", resolver.getSuffix());
        assertEquals("UTF-8", resolver.getCharacterEncoding());
        assertEquals(Set.of("thymeleaflet/**"), resolver.getResolvablePatterns());
        assertEquals(Integer.valueOf(0), resolver.getOrder());
        assertFalse(resolver.getCheckExistence(), "Resolver should open the classpath resource directly");
    }

    @Test
    void resolvedStorybookConfig_shouldDisableThymeleafletCacheWhenThymeleafCacheIsDisabledByDevelopmentConfig() {
        StorybookAutoConfiguration configuration = new StorybookAutoConfiguration();
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.thymeleaf.cache", "false");

        ResolvedStorybookConfig resolved = configuration.resolvedStorybookConfig(new StorybookProperties(), environment);

        assertFalse(resolved.getCache().isEnabled());
    }

    @Test
    void resolvedStorybookConfig_shouldRespectExplicitThymeleafletCacheSetting() {
        StorybookAutoConfiguration configuration = new StorybookAutoConfiguration();
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.thymeleaf.cache", "false")
            .withProperty("thymeleaflet.cache.enabled", "true");

        ResolvedStorybookConfig resolved = configuration.resolvedStorybookConfig(new StorybookProperties(), environment);

        assertTrue(resolved.getCache().isEnabled());
    }

    @Test
    @DisplayName("thymeleafletObjectMapper should serialize LocalDateTime without timestamp")
    void thymeleafletObjectMapperBean_shouldSupportJavaTime() throws Exception {
        StorybookAutoConfiguration configuration = new StorybookAutoConfiguration();
        ObjectMapper objectMapper = configuration.thymeleafletObjectMapper();

        String json = objectMapper.writeValueAsString(LocalDateTime.of(2026, 1, 1, 0, 0));

        assertEquals("\"2026-01-01T00:00:00\"", json);
    }
}
