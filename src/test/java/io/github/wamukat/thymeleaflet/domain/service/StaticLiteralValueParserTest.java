package io.github.wamukat.thymeleaflet.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StaticLiteralValueParserTest {

    @Test
    void shouldParseSafeStaticLiterals() {
        assertThat(StaticLiteralValueParser.parse("'Ready'")).contains("Ready");
        assertThat(StaticLiteralValueParser.parse("\"Ready\"")).contains("Ready");
        assertThat(StaticLiteralValueParser.parse("true")).contains(true);
        assertThat(StaticLiteralValueParser.parse("42")).contains(42);
    }

    @Test
    void shouldRejectDynamicExpressionsAndUnquotedStringsByDefault() {
        assertThat(StaticLiteralValueParser.parse("${dynamic}")).isEmpty();
        assertThat(StaticLiteralValueParser.parse("*{field}")).isEmpty();
        assertThat(StaticLiteralValueParser.parse("@{/path}")).isEmpty();
        assertThat(StaticLiteralValueParser.parse("#{message.key}")).isEmpty();
        assertThat(StaticLiteralValueParser.parse("default")).isEmpty();
    }

    @Test
    void shouldAllowUnquotedStringDefaultsWhenTypeIsString() {
        assertThat(StaticLiteralValueParser.parse("primary", "String")).contains("primary");
        assertThat(StaticLiteralValueParser.parse("primary", "java.lang.String")).contains("primary");
        assertThat(StaticLiteralValueParser.parse("primary", "Enum")).isEmpty();
    }
}
