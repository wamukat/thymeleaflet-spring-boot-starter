package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionExposureDiagnostics implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProductionExposureDiagnostics.class);

    private final Environment environment;
    private final ResolvedStorybookConfig storybookConfig;

    public ProductionExposureDiagnostics(Environment environment, ResolvedStorybookConfig storybookConfig) {
        this.environment = environment;
        this.storybookConfig = storybookConfig;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String warning : warningMessages(environment, storybookConfig)) {
            logger.warn(warning);
        }
    }

    static List<String> warningMessages(Environment environment, ResolvedStorybookConfig storybookConfig) {
        if (!hasProductionProfile(environment)) {
            return List.of();
        }

        List<String> warnings = new ArrayList<>();
        warnings.add(
            "Thymeleaflet is active under a production profile. This tool is intended for development; "
                + "set thymeleaflet.enabled=false, exclude StorybookAutoConfiguration, or remove the dependency "
                + "from production builds."
        );
        if (storybookConfig.getSecurity().isAutoPermit()) {
            warnings.add(
                "thymeleaflet.security.auto-permit=true is active under a production profile and permits "
                    + storybookConfig.getBasePath() + "/**. Prefer app-owned authentication rules or disable "
                    + "Thymeleaflet in production."
            );
        }
        return List.copyOf(warnings);
    }

    private static boolean hasProductionProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
            .map(String::trim)
            .map(String::toLowerCase)
            .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
    }
}
