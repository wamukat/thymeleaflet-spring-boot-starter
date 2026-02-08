package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import org.thymeleaf.standard.expression.FragmentSignature;
import org.thymeleaf.standard.expression.FragmentSignatureUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

@Component
public class FragmentSignatureParser {

    private static final Method INTERNAL_PARSE_METHOD = loadInternalParseMethod();

    public ParseResult parse(String definition) {
        Objects.requireNonNull(definition, "definition cannot be null");
        String input = definition.trim();
        if (input.isEmpty()) {
            return new ParseError(DiagnosticCode.INVALID_SIGNATURE, "fragment definition is empty");
        }
        if (hasEmptyParameterToken(input)) {
            return new ParseError(DiagnosticCode.INVALID_SIGNATURE, "empty parameter token");
        }

        final FragmentSignature signature;
        try {
            signature = (FragmentSignature) INTERNAL_PARSE_METHOD.invoke(null, input);
        } catch (Exception ex) {
            return new ParseError(DiagnosticCode.INVALID_SIGNATURE, "invalid signature: " + input);
        }
        if (signature == null) {
            return new ParseError(DiagnosticCode.INVALID_SIGNATURE, "invalid signature: " + input);
        }

        String fragmentName = signature.getFragmentName();
        if (!isValidIdentifier(fragmentName)) {
            return new ParseError(DiagnosticCode.UNSUPPORTED_SYNTAX, "unsupported fragment name syntax: " + fragmentName);
        }

        List<String> parameters = signature.hasParameters() ? signature.getParameterNames() : List.of();
        for (String token : parameters) {
            if (token.isEmpty()) {
                return new ParseError(DiagnosticCode.INVALID_SIGNATURE, "empty parameter token");
            }
            if (!isValidIdentifier(token)) {
                return new ParseError(DiagnosticCode.UNSUPPORTED_SYNTAX, "unsupported parameter syntax: " + token);
            }
        }

        return new ParseSuccess(fragmentName, parameters);
    }

    private static boolean isValidIdentifier(String value) {
        if (value.isEmpty()) {
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

    private static Method loadInternalParseMethod() {
        try {
            Method method = FragmentSignatureUtils.class.getDeclaredMethod("internalParseFragmentSignature", String.class);
            method.setAccessible(true);
            return method;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to access Thymeleaf fragment signature parser", ex);
        }
    }

    private static boolean hasEmptyParameterToken(String input) {
        int openIndex = input.indexOf('(');
        int closeIndex = input.lastIndexOf(')');
        if (openIndex < 0 || closeIndex <= openIndex) {
            return false;
        }
        String compact = input.substring(openIndex + 1, closeIndex).replaceAll("\\s+", "");
        return compact.startsWith(",") || compact.endsWith(",") || compact.contains(",,");
    }

    public sealed interface ParseResult permits ParseSuccess, ParseError {
    }

    public record ParseSuccess(String fragmentName, List<String> parameters) implements ParseResult {
        public ParseSuccess {
            fragmentName = fragmentName.trim();
            parameters = List.copyOf(parameters);
        }
    }

    public record ParseError(DiagnosticCode code, String message) implements ParseResult {
    }
}
