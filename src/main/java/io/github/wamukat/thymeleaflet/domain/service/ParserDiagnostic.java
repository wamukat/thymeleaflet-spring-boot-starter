package io.github.wamukat.thymeleaflet.domain.service;

import java.util.Objects;

public record ParserDiagnostic(String code, String message, int line, int column) {

    public ParserDiagnostic {
        code = Objects.requireNonNull(code, "code cannot be null");
        message = Objects.requireNonNull(message, "message cannot be null");
    }

    public static ParserDiagnostic warning(String code, String message) {
        return new ParserDiagnostic(code, message, -1, -1);
    }
}
