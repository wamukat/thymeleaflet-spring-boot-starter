package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
