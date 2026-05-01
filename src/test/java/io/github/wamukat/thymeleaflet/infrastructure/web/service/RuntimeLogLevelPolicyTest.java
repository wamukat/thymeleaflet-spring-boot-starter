package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RuntimeLogLevelPolicyTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void successPathRuntimeLogsStayBelowInfoLevel() throws IOException {
        try (Stream<Path> paths = Stream.of(
                sourcePath("infrastructure/web/service/FragmentRenderingService.java"),
                sourcePath("infrastructure/web/service/StoryPreviewService.java"),
                sourcePath("infrastructure/web/service/StoryContentService.java"),
                sourcePath("infrastructure/web/service/SecurePathConversionService.java"),
                sourcePath("infrastructure/web/service/StoryCommonDataService.java"),
                sourcePath("infrastructure/web/service/UsageExampleService.java"),
                sourcePath("infrastructure/web/service/FragmentMainContentService.java"),
                sourcePath("infrastructure/web/service/FragmentJsonService.java"),
                sourcePath("infrastructure/web/controller/FragmentListController.java"),
                sourcePath("infrastructure/adapter/documentation/TypeInformationExtractor.java"),
                sourcePath("application/service/fragment/FragmentMetricsService.java"),
                sourcePath("application/service/fragment/FragmentValidationService.java"),
                sourcePath("application/service/story/StoryParameterUseCaseImpl.java"),
                sourcePath("application/service/preview/FragmentPreviewUseCaseImpl.java"),
                sourcePath("application/service/coordination/StoryPageCoordinationUseCaseImpl.java"),
                sourcePath("application/service/coordination/StoryContentCoordinationUseCaseImpl.java"),
                sourcePath("domain/model/SecureTemplatePath.java"))) {
            assertThat(paths)
                    .allSatisfy(path -> assertThat(Files.readString(path))
                            .as("%s should not log normal runtime details at INFO", path)
                            .doesNotContain("logger.info("));
        }
    }

    @Test
    void diagnosticRuntimeLogsAreRetained() throws IOException {
        assertThat(Files.readString(sourcePath("infrastructure/web/service/FragmentRenderingService.java")))
                .contains("logger.error(");
        assertThat(Files.readString(sourcePath("infrastructure/web/service/StoryPreviewService.java")))
                .contains("logger.error(");
        assertThat(Files.readString(sourcePath("infrastructure/web/service/StoryContentService.java")))
                .contains("logger.error(");
        assertThat(Files.readString(sourcePath("infrastructure/web/service/SecurePathConversionService.java")))
                .contains("logger.error(");
        assertThat(Files.readString(sourcePath("domain/model/SecureTemplatePath.java")))
                .contains("logger.warn(");
    }

    private static Path sourcePath(String packageRelativePath) {
        return PROJECT_ROOT.resolve("src/main/java/io/github/wamukat/thymeleaflet").resolve(packageRelativePath);
    }
}
