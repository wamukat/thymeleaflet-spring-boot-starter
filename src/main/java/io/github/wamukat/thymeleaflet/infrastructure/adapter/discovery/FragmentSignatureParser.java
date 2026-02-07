package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FragmentSignatureParser {

    public ParseResult parse(String definition) {
        if (definition == null) {
            return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "fragment definition is null");
        }

        String input = definition.trim();
        if (input.isEmpty()) {
            return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "fragment definition is empty");
        }

        int openParenIndex = input.indexOf('(');
        if (openParenIndex < 0) {
            if (!isValidIdentifier(input)) {
                return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "invalid fragment name: " + input);
            }
            return ParseResult.success(input, List.of());
        }

        int closeParenIndex = input.lastIndexOf(')');
        if (closeParenIndex < openParenIndex) {
            return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "unbalanced parentheses");
        }

        String trailing = input.substring(closeParenIndex + 1).trim();
        if (!trailing.isEmpty()) {
            return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "unexpected trailing content");
        }

        String fragmentName = input.substring(0, openParenIndex).trim();
        if (!isValidIdentifier(fragmentName)) {
            return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "invalid fragment name: " + fragmentName);
        }

        String paramSection = input.substring(openParenIndex + 1, closeParenIndex).trim();
        if (paramSection.isEmpty()) {
            return ParseResult.success(fragmentName, List.of());
        }

        List<String> parameters = new ArrayList<>();
        for (String rawToken : splitByComma(paramSection)) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "empty parameter token");
            }
            if (!isValidIdentifier(token)) {
                if (token.contains("=") || token.contains("'") || token.contains("\"")) {
                    return ParseResult.error(DiagnosticCode.UNSUPPORTED_SYNTAX, "unsupported parameter syntax: " + token);
                }
                return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "invalid parameter token: " + token);
            }
            parameters.add(token);
        }

        return ParseResult.success(fragmentName, parameters);
    }

    private static List<String> splitByComma(String value) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ',') {
                result.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        result.add(current.toString());
        return result;
    }

    private static boolean isValidIdentifier(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        char first = value.charAt(0);
        if (!isIdentifierStart(first)) {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            if (!isIdentifierPart(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIdentifierStart(char ch) {
        return Character.isLetterOrDigit(ch);
    }

    private static boolean isIdentifierPart(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '-';
    }

    public enum DiagnosticCode {
        INVALID_SIGNATURE,
        UNSUPPORTED_SYNTAX
    }

    public record ParseResult(boolean success, String fragmentName, List<String> parameters, DiagnosticCode code,
                              String message) {
        public static ParseResult success(String fragmentName, List<String> parameters) {
            return new ParseResult(true, fragmentName, List.copyOf(parameters), null, null);
        }

        public static ParseResult error(DiagnosticCode code, String message) {
            return new ParseResult(false, null, List.of(), code, message);
        }
    }
}
