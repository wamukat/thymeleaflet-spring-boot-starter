package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FragmentSignatureParser Tests")
class FragmentSignatureParserTest {

    private final FragmentSignatureParser parser = new FragmentSignatureParser();

    @Test
    @DisplayName("name only signature is parsed")
    void parsesNameOnly() {
        FragmentSignatureParser.ParseResult result = parser.parse("profileCard");

        assertThat(result).isInstanceOf(FragmentSignatureParser.ParseSuccess.class);
        FragmentSignatureParser.ParseSuccess success = (FragmentSignatureParser.ParseSuccess) result;
        assertThat(success.fragmentName()).isEqualTo("profileCard");
        assertThat(success.parameters()).isEmpty();
    }

    @Test
    @DisplayName("empty parameter list is parsed")
    void parsesEmptyParameterList() {
        FragmentSignatureParser.ParseResult result = parser.parse("profileCard()");

        assertThat(result).isInstanceOf(FragmentSignatureParser.ParseSuccess.class);
        FragmentSignatureParser.ParseSuccess success = (FragmentSignatureParser.ParseSuccess) result;
        assertThat(success.fragmentName()).isEqualTo("profileCard");
        assertThat(success.parameters()).isEmpty();
    }

    @Test
    @DisplayName("parameter list keeps declaration order")
    void parsesParameterList() {
        FragmentSignatureParser.ParseResult result = parser.parse("profileCard(name, age)");

        assertThat(result).isInstanceOf(FragmentSignatureParser.ParseSuccess.class);
        FragmentSignatureParser.ParseSuccess success = (FragmentSignatureParser.ParseSuccess) result;
        assertThat(success.fragmentName()).isEqualTo("profileCard");
        assertThat(success.parameters()).containsExactly("name", "age");
    }

    @Test
    @DisplayName("whitespace around signature is normalized")
    void parsesWithWhitespace() {
        FragmentSignatureParser.ParseResult result = parser.parse(" profileCard ( name , age ) ");

        assertThat(result).isInstanceOf(FragmentSignatureParser.ParseSuccess.class);
        FragmentSignatureParser.ParseSuccess success = (FragmentSignatureParser.ParseSuccess) result;
        assertThat(success.fragmentName()).isEqualTo("profileCard");
        assertThat(success.parameters()).containsExactly("name", "age");
    }

    @Test
    @DisplayName("empty token is invalid")
    void rejectsEmptyToken() {
        FragmentSignatureParser.ParseResult result = parser.parse("profileCard(name,,age)");

        assertThat(result).isInstanceOf(FragmentSignatureParser.ParseError.class);
        FragmentSignatureParser.ParseError error = (FragmentSignatureParser.ParseError) result;
        assertThat(error.code()).isEqualTo(FragmentSignatureParser.DiagnosticCode.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("unbalanced parenthesis is invalid")
    void rejectsUnbalancedParenthesis() {
        FragmentSignatureParser.ParseResult result = parser.parse("profileCard(name");

        assertThat(result).isInstanceOf(FragmentSignatureParser.ParseError.class);
        FragmentSignatureParser.ParseError error = (FragmentSignatureParser.ParseError) result;
        assertThat(error.code()).isEqualTo(FragmentSignatureParser.DiagnosticCode.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("assignment style parameter is unsupported")
    void rejectsAssignmentStyle() {
        FragmentSignatureParser.ParseResult result = parser.parse("profileCard(name='x')");

        assertThat(result).isInstanceOf(FragmentSignatureParser.ParseError.class);
        FragmentSignatureParser.ParseError error = (FragmentSignatureParser.ParseError) result;
        assertThat(error.code()).isEqualTo(FragmentSignatureParser.DiagnosticCode.UNSUPPORTED_SYNTAX);
    }
}
