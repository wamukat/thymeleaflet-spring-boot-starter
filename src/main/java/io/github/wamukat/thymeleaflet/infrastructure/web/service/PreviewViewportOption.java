package io.github.wamukat.thymeleaflet.infrastructure.web.service;

public class PreviewViewportOption {
    private final String id;
    private final String label;
    private final int width;
    private final int height;

    public PreviewViewportOption(String id, String label, int width, int height) {
        this.id = id;
        this.label = label;
        this.width = width;
        this.height = height;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
