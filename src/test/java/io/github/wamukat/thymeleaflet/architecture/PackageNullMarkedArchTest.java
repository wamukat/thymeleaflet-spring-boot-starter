package io.github.wamukat.thymeleaflet.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PackageNullMarkedArchTest {

    private static final Path MAIN_JAVA_ROOT = Path.of("src/main/java");
    private static final Path TEST_JAVA_ROOT = Path.of("src/test/java");
    private static final String NULL_MARKED_ANNOTATION = "@org.jspecify.annotations.NullMarked";

    @Test
    void allJavaPackagesShouldDeclareNullMarkedInPackageInfo() throws IOException {
        Set<Path> packageDirectories = collectPackageDirectories(MAIN_JAVA_ROOT);
        packageDirectories.addAll(collectPackageDirectories(TEST_JAVA_ROOT));
        Set<Path> missingPackageInfo = new TreeSet<>();
        Set<Path> missingNullMarked = new TreeSet<>();

        for (Path packageDir : packageDirectories) {
            Path packageInfo = packageDir.resolve("package-info.java");
            if (!Files.exists(packageInfo)) {
                missingPackageInfo.add(packageDir);
                continue;
            }
            List<String> lines = Files.readAllLines(packageInfo);
            boolean hasNullMarked = lines.stream().anyMatch(line -> line.contains(NULL_MARKED_ANNOTATION));
            if (!hasNullMarked) {
                missingNullMarked.add(packageInfo);
            }
        }

        assertThat(missingPackageInfo)
            .as("package-info.java must exist for all src/main and src/test packages")
            .isEmpty();
        assertThat(missingNullMarked)
            .as("package-info.java must declare @NullMarked for all src/main and src/test packages")
            .isEmpty();
    }

    private Set<Path> collectPackageDirectories(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files
                .filter(path -> path.toString().endsWith(".java"))
                .map(Path::getParent)
                .filter(path -> path != null && !path.equals(root))
                .collect(TreeSet::new, Set::add, Set::addAll);
        }
    }
}
