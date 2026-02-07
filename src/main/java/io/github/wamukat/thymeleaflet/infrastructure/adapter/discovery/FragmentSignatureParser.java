package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import org.thymeleaf.standard.expression.FragmentSignature;
import org.thymeleaf.standard.expression.FragmentSignatureUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

@Component
public class FragmentSignatureParser {

    private static final Method INTERNAL_PARSE_METHOD = loadInternalParseMethod();

    public ParseResult parse(String definition) {
        if (definition == null) {
            return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "fragment definition is null");
        }

        String input = definition.trim();
        if (input.isEmpty()) {
            return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "fragment definition is empty");
        }
        if (hasEmptyParameterToken(input)) {
            return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "empty parameter token");
        }

        final FragmentSignature signature;
        try {
            signature = (FragmentSignature) INTERNAL_PARSE_METHOD.invoke(null, input);
        } catch (Exception ex) {
            return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "invalid signature: " + input);
        }
        if (signature == null) {
            return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "invalid signature: " + input);
        }

        String fragmentName = signature.getFragmentName();
        if (!isValidIdentifier(fragmentName)) {
            return ParseResult.error(DiagnosticCode.UNSUPPORTED_SYNTAX, "unsupported fragment name syntax: " + fragmentName);
        }

        List<String> parameters = signature.hasParameters() ? signature.getParameterNames() : List.of();
        for (String token : parameters) {
            if (token.isEmpty()) {
                return ParseResult.error(DiagnosticCode.INVALID_SIGNATURE, "empty parameter token");
            }
            if (!isValidIdentifier(token)) {
                return ParseResult.error(DiagnosticCode.UNSUPPORTED_SYNTAX, "unsupported parameter syntax: " + token);
            }
        }

        return ParseResult.success(fragmentName, parameters);
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
