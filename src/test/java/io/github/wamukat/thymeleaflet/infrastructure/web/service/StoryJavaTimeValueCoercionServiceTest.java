package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StoryJavaTimeValueCoercionService Tests")
class StoryJavaTimeValueCoercionServiceTest {

    private final StoryJavaTimeValueCoercionService service = new StoryJavaTimeValueCoercionService();

    @Test
    @DisplayName("@param の java.time.LocalDateTime 型に合わせて parameters の ISO 文字列を変換する")
    void shouldCoerceLocalDateTimeParameterFromJavaDocParamType() {
        JavaDocAnalyzer.JavaDocInfo javaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "Detail header",
            List.of(JavaDocAnalyzer.ParameterInfo.required("publishedAt", "java.time.LocalDateTime", "公開日時")),
            List.of(),
            Optional.empty()
        );

        Map<String, Object> result = service.coerceParameters(
            Map.of("publishedAt", "2026-04-01T10:00:00", "title", "Campaign"),
            javaDocInfo
        );

        assertThat(result.get("publishedAt")).isEqualTo(LocalDateTime.of(2026, 4, 1, 10, 0));
        assertThat(result.get("title")).isEqualTo("Campaign");
    }

    @Test
    @DisplayName("@model の [] パスに合わせて list 内の model フィールドを変換する")
    void shouldCoerceNestedListModelValuesFromJavaDocModelPath() {
        JavaDocAnalyzer.JavaDocInfo javaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "List",
            List.of(),
            List.of(JavaDocAnalyzer.ModelInfo.required(
                "view.items[].publishedAt",
                "LocalDateTime",
                "公開日時"
            )),
            List.of(),
            Optional.empty()
        );
        Map<String, Object> model = Map.of(
            "view",
            Map.of(
                "items",
                List.of(
                    Map.of("title", "Notice 1", "publishedAt", "2024-06-01T10:00:00"),
                    Map.of("title", "Notice 2", "publishedAt", "2024-06-02T11:30:00")
                )
            )
        );

        Map<String, Object> result = service.coerceModel(model, javaDocInfo);

        assertThat(result.get("view")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> view = (Map<String, Object>) Objects.requireNonNull(result.get("view"));
        assertThat(view.get("items")).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) Objects.requireNonNull(view.get("items"));
        assertThat(items)
            .extracting(item -> item.get("publishedAt"))
            .containsExactly(
                LocalDateTime.of(2024, 6, 1, 10, 0),
                LocalDateTime.of(2024, 6, 2, 11, 30)
            );
        assertThat(items).extracting(item -> item.get("title")).containsExactly("Notice 1", "Notice 2");
    }

    @Test
    @DisplayName("java.time 以外の型は既存値を変更しない")
    void shouldLeaveUnsupportedTypesUnchanged() {
        JavaDocAnalyzer.JavaDocInfo javaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "Detail header",
            List.of(JavaDocAnalyzer.ParameterInfo.required("title", "String", "タイトル")),
            List.of(),
            Optional.empty()
        );

        Map<String, Object> result = service.coerceParameters(Map.of("title", "Campaign"), javaDocInfo);

        assertThat(result).containsEntry("title", "Campaign");
    }

    @Test
    @DisplayName("java.time 型の不正な値は対象 path と型が分かる例外にする")
    void shouldReportClearErrorForInvalidJavaTimeValue() {
        JavaDocAnalyzer.JavaDocInfo javaDocInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "Detail header",
            List.of(JavaDocAnalyzer.ParameterInfo.required("publishedAt", "java.time.LocalDateTime", "公開日時")),
            List.of(),
            Optional.empty()
        );

        assertThatThrownBy(() -> service.coerceParameters(Map.of("publishedAt", "not-a-date"), javaDocInfo))
            .isInstanceOf(StoryJavaTimeValueCoercionService.StoryValueCoercionException.class)
            .hasMessageContaining("publishedAt")
            .hasMessageContaining("java.time.LocalDateTime")
            .hasMessageContaining("not-a-date");
    }
}
