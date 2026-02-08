package io.github.wamukat.thymeleaflet.application.port.outbound;

import java.util.Optional;

/**
 * JavaDoc情報の参照ポート。
 */
public interface JavaDocLookupPort {

    Optional<Object> findJavaDocInfoForView(String templatePath, String fragmentName);

    boolean hasJavaDoc(String templatePath, String fragmentName);
}
