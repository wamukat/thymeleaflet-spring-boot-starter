package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.web.controller.FragmentListController;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

@Component
public class ThymeleafletVersionResolver {

    public String resolve() {
        String version = Optional.ofNullable(FragmentListController.class.getPackage().getImplementationVersion())
            .filter(v -> !v.isBlank())
            .orElse("");
        if (!version.isEmpty()) {
            return version;
        }

        try (InputStream inputStream = FragmentListController.class.getClassLoader()
            .getResourceAsStream("META-INF/maven/io.github.wamukat/thymeleaflet-spring-boot-starter/pom.properties")) {
            if (inputStream == null) {
                return "x.y.z";
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            String pomVersion = Optional.ofNullable(properties.getProperty("version"))
                .filter(v -> !v.isBlank())
                .orElse("");
            if (!pomVersion.isEmpty()) {
                return pomVersion;
            }
        } catch (Exception ignored) {
            // Fallback to default label below.
        }

        return "x.y.z";
    }
}
