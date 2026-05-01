package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class TemplateScanner {

    private final ResolvedStorybookConfig storybookConfig;
    private final ResourcePatternResolver resourceResolver;

    @Autowired
    public TemplateScanner(ResolvedStorybookConfig storybookConfig) {
        this(storybookConfig, new PathMatchingResourcePatternResolver());
    }

    TemplateScanner(ResolvedStorybookConfig storybookConfig, ResourcePatternResolver resourceResolver) {
        this.storybookConfig = Objects.requireNonNull(storybookConfig, "storybookConfig cannot be null");
        this.resourceResolver = Objects.requireNonNull(resourceResolver, "resourceResolver cannot be null");
    }

    public List<TemplateResource> scanTemplates() throws IOException {
        List<TemplateResource> templates = new ArrayList<>();
        for (String templatePath : storybookConfig.getResources().getTemplatePaths()) {
            String searchPattern = "classpath:" + templatePath + "**/*.html";
            Resource[] resources = resourceResolver.getResources(searchPattern);

            for (Resource resource : resources) {
                String resourceUri = resource.getURI().toString();
                String relativeTemplatePath = extractTemplatePath(resourceUri);
                String content;
                try (var inputStream = resource.getInputStream()) {
                    content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
                templates.add(new TemplateResource(relativeTemplatePath, content, resourceUri));
            }
        }
        return List.copyOf(templates);
    }

    private String extractTemplatePath(String resourceUri) {
        for (String templatePath : storybookConfig.getResources().getTemplatePaths()) {
            String pathWithoutSlash = templatePath.substring(1);
            int index = resourceUri.indexOf(pathWithoutSlash);
            if (index != -1) {
                return resourceUri.substring(index + pathWithoutSlash.length()).replace(".html", "");
            }
        }

        int index = resourceUri.indexOf("templates/");
        if (index != -1) {
            return resourceUri.substring(index + "templates/".length()).replace(".html", "");
        }

        return resourceUri;
    }

    public record TemplateResource(String templatePath, String content, String uri) {
        public TemplateResource {
            templatePath = templatePath.trim();
            content = Objects.requireNonNull(content, "content cannot be null");
            uri = Objects.requireNonNull(uri, "uri cannot be null");
        }
    }
}
