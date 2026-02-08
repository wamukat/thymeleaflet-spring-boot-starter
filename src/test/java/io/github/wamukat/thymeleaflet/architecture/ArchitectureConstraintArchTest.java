package io.github.wamukat.thymeleaflet.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import io.github.wamukat.thymeleaflet.application.port.outbound.FragmentCatalogPort;
import io.github.wamukat.thymeleaflet.application.port.outbound.FragmentDependencyPort;
import io.github.wamukat.thymeleaflet.application.port.outbound.JavaDocLookupPort;
import io.github.wamukat.thymeleaflet.application.port.outbound.StoryPresentationPort;
import io.github.wamukat.thymeleaflet.application.port.outbound.DocumentationAnalysisPort;
import io.github.wamukat.thymeleaflet.application.port.outbound.StoryDataPort;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "io.github.wamukat.thymeleaflet",
    importOptions = {com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests.class}
)
class ArchitectureConstraintArchTest {

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule ports_should_be_interfaces =
        classes()
            .that()
            .areTopLevelClasses()
            .and()
            .resideInAnyPackage(
                "..application.port.inbound..",
                "..application.port.outbound.."
            )
            .should()
            .beInterfaces();

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule storybook_properties_should_stay_in_configuration_boundary =
        noClasses()
            .that()
            .resideOutsideOfPackage("..infrastructure.configuration..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(StorybookProperties.class.getName());

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule domain_should_not_depend_on_application_or_infrastructure =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule domain_model_should_be_framework_agnostic =
        noClasses()
            .that()
            .resideInAnyPackage("..domain.model..", "..domain.model.configuration..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.servlet..");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule outbound_port_implementations_should_live_in_adapter_layer =
        classes()
            .that()
            .implement(StoryDataPort.class)
            .or()
            .implement(DocumentationAnalysisPort.class)
            .or()
            .implement(FragmentCatalogPort.class)
            .or()
            .implement(StoryPresentationPort.class)
            .or()
            .implement(JavaDocLookupPort.class)
            .or()
            .implement(FragmentDependencyPort.class)
            .should()
            .resideInAnyPackage("..infrastructure.adapter..", "..infrastructure.web..");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule application_ports_should_not_depend_on_infrastructure =
        noClasses()
            .that()
            .resideInAnyPackage("..application.port.inbound..", "..application.port.outbound..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule domain_should_not_use_spring_stereotypes =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .beAnnotatedWith(Component.class)
            .orShould()
            .beAnnotatedWith(Service.class)
            .orShould()
            .beAnnotatedWith(Repository.class)
            .orShould()
            .beAnnotatedWith(Controller.class);

}
