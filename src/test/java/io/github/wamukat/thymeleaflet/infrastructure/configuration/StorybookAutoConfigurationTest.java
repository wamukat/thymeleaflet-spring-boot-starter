package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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
}
