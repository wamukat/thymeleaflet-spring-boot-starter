package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaDocCommentBlockExtractor {

    private static final Pattern JAVADOC_PATTERN = Pattern.compile(
        "<!--[^>]*?/\\*\\*([\\s\\S]*?)\\*/[^<]*?-->",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    List<String> extract(String htmlContent) {
        List<String> blocks = new ArrayList<>();
        Matcher javadocMatcher = JAVADOC_PATTERN.matcher(htmlContent);
        while (javadocMatcher.find()) {
            blocks.add(javadocMatcher.group(1));
        }
        return blocks;
    }
}
