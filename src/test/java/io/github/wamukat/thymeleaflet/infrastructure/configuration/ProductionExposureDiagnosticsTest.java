package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionExposureDiagnosticsTest {

    @Test
    void warningMessages_shouldWarnWhenThymeleafletIsActiveUnderProductionProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        List<String> warnings = ProductionExposureDiagnostics.warningMessages(
            environment,
            ResolvedStorybookConfig.from(new StorybookProperties())
        );

        assertThat(warnings)
            .anyMatch(message -> message.contains("production profile"));
    }

    @Test
    void warningMessages_shouldWarnWhenAutoPermitIsActiveUnderProductionProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        StorybookProperties properties = new StorybookProperties();
        StorybookProperties.SecurityConfig security = new StorybookProperties.SecurityConfig();
        security.setAutoPermit(true);
        properties.setSecurity(security);

        List<String> warnings = ProductionExposureDiagnostics.warningMessages(
            environment,
            ResolvedStorybookConfig.from(properties)
        );

        assertThat(warnings)
            .anyMatch(message -> message.contains("thymeleaflet.security.auto-permit=true"));
    }

    @Test
    void warningMessages_shouldStayQuietForDevelopmentProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        List<String> warnings = ProductionExposureDiagnostics.warningMessages(
            environment,
            ResolvedStorybookConfig.from(new StorybookProperties())
        );

        assertThat(warnings).isEmpty();
    }
}
