package io.github.wamukat.thymeleaflet.documentation;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ReadmeVersionConsistencyTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private static final String GROUP_ID = "io.github.wamukat";
    private static final String ARTIFACT_ID = "thymeleaflet-spring-boot-starter";

    @Test
    void readmeDependencyExamplesUseProjectVersion() throws Exception {
        String projectVersion = readProjectVersion();

        assertReadmeUsesVersion(PROJECT_ROOT.resolve("README.md"), projectVersion);
        assertReadmeUsesVersion(PROJECT_ROOT.resolve("README.ja.md"), projectVersion);
    }

    private static String readProjectVersion() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder().parse(PROJECT_ROOT.resolve("pom.xml").toFile());
        Element project = document.getDocumentElement();

        for (Node child = project.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && "version".equals(child.getNodeName())) {
                return Objects.requireNonNull(child.getTextContent()).trim();
            }
        }
        throw new IllegalStateException("Project version not found in pom.xml");
    }

    private static void assertReadmeUsesVersion(Path readmePath, String projectVersion) throws Exception {
        String readme = Files.readString(readmePath);
        assertThat(readme)
            .as("%s Maven dependency version", readmePath.getFileName())
            .contains("""
                <dependency>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </dependency>
                """.formatted(GROUP_ID, ARTIFACT_ID, projectVersion));
        assertThat(readme)
            .as("%s Gradle dependency version", readmePath.getFileName())
            .contains("implementation(\"" + GROUP_ID + ":" + ARTIFACT_ID + ":" + projectVersion + "\")");
    }
}
