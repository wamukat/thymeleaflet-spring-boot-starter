package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.web.controller.FragmentListController;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Properties;

@Component
public class ThymeleafletVersionResolver {

    public String resolve() {
        String version = FragmentListController.class.getPackage().getImplementationVersion();
        if (version != null && !version.isBlank()) {
            return version;
        }

        try (InputStream inputStream = FragmentListController.class.getClassLoader()
            .getResourceAsStream("META-INF/maven/io.github.wamukat/thymeleaflet-spring-boot-starter/pom.properties")) {
            if (inputStream != null) {
                Properties properties = new Properties();
                properties.load(inputStream);
                String pomVersion = properties.getProperty("version");
                if (pomVersion != null && !pomVersion.isBlank()) {
                    return pomVersion;
                }
            }
        } catch (Exception ignored) {
            // Fallback to default label below.
        }

        return "x.y.z";
    }
}
