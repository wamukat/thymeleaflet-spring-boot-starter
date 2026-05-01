package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ITemplateResolver;

class ThymeleafletClassLoaderTemplateResolverIntegrationTest {

    @Test
    void thymeleafletClassLoaderTemplateResolver_shouldRenderFragmentListFromStarterJarForDevToolsRestartClassLoader(
        @TempDir Path tempDir
    ) throws Exception {
        Path starterJar = createStarterTemplateJar(tempDir);
        ITemplateResolver templateResolver = new StorybookAutoConfiguration()
            .thymeleafletClassLoaderTemplateResolver();
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();

        // DevTools runs application code in a restart class loader while starter resources stay in a parent/JAR.
        // This regression test proves thymeleaflet/** templates render from the class loader, not a web resource path.
        try (URLClassLoader starterJarClassLoader = new StarterResourceFirstClassLoader(
            new URL[] {starterJar.toUri().toURL()},
            ThymeleafletClassLoaderTemplateResolverIntegrationTest.class.getClassLoader()
        )) {
            currentThread.setContextClassLoader(starterJarClassLoader);

            String html = templateEngine.process(
                "thymeleaflet/fragment-list",
                new Context(Locale.ENGLISH)
            );

            assertTrue(
                html.contains("fixture fragment list from starter jar"),
                "thymeleaflet/fragment-list should be rendered from the starter JAR class loader"
            );
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
        }
    }

    private static Path createStarterTemplateJar(Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("thymeleaflet-starter-template-fixture.jar");
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jarOutputStream.putNextEntry(new JarEntry("templates/thymeleaflet/fragment-list.html"));
            jarOutputStream.write("""
                <!DOCTYPE html>
                <html xmlns:th="http://www.thymeleaf.org">
                <body>
                    <main th:text="'fixture fragment list from starter jar'">
                        fallback
                    </main>
                </body>
                </html>
                """.getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();
        }
        return jarPath;
    }

    private static final class StarterResourceFirstClassLoader extends URLClassLoader {
        private static final String STARTER_TEMPLATE_PREFIX = "templates/thymeleaflet/";

        private StarterResourceFirstClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public @Nullable URL getResource(String name) {
            if (name.startsWith(STARTER_TEMPLATE_PREFIX)) {
                URL starterResource = findResource(name);
                if (starterResource != null) {
                    return starterResource;
                }
            }
            return super.getResource(name);
        }

        @Override
        public @Nullable InputStream getResourceAsStream(String name) {
            try {
                URL resource = getResource(name);
                if (resource != null) {
                    return resource.openStream();
                }
            } catch (IOException ex) {
                return null;
            }
            return null;
        }
    }
}
