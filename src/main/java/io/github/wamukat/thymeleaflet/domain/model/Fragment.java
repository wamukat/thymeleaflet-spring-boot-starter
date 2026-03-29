package io.github.wamukat.thymeleaflet.domain.model;

import java.util.*;

/**
 * フラグメント - Domain Entity (Aggregate Root)
 * 
 * Thymeleaf フラグメントを表現するドメインの中心的エンティティ
 * Clean Architecture の原則に従い、ビジネスルールとドメイン不変性を保持
 */
public class Fragment {

    private final SecureTemplatePath templatePath;
    private final FragmentName name;
    private final String category;
    private final Map<StoryName, Story> stories;
    private final Set<String> requiredParameters;
    private final Map<String, Object> metadata;

    /**
     * プライベートコンストラクタ - 不変Value Object設計
     * 
     * ファクトリメソッドのみからのインスタンス化を強制
     * Clean Architecture: 検証済み値による安全なオブジェクト生成
     */
    private Fragment(SecureTemplatePath templatePath, 
                   FragmentName name, 
                   String category,
                   Map<StoryName, Story> stories,
                   Set<String> requiredParameters,
                   Map<String, Object> metadata) {
        this.templatePath = Objects.requireNonNull(templatePath, "Template path cannot be null");
        this.name = Objects.requireNonNull(name, "Fragment name cannot be null");
        this.category = Objects.requireNonNullElse(category, "component");
        
        // 防御的コピー + 不変化
        this.stories = Collections.unmodifiableMap(
            new LinkedHashMap<>(Objects.requireNonNullElse(stories, Collections.emptyMap())));
        this.requiredParameters = Collections.unmodifiableSet(
            new LinkedHashSet<>(Objects.requireNonNullElse(requiredParameters, Collections.emptySet())));
        this.metadata = Collections.unmodifiableMap(
            new HashMap<>(Objects.requireNonNullElse(metadata, Collections.emptyMap())));
    }

    /**
     * Fragment作成 - ファクトリメソッド（完全指定版）
     */
    public static Fragment of(SecureTemplatePath templatePath, 
                            FragmentName name, 
                            String category,
                            Map<StoryName, Story> stories,
                            Set<String> requiredParameters,
                            Map<String, Object> metadata) {
        return new Fragment(templatePath, name, category, stories, requiredParameters, metadata);
    }

    /**
     * Fragment作成 - ファクトリメソッド（基本版）
     */
    public static Fragment of(SecureTemplatePath templatePath, FragmentName name) {
        return new Fragment(
            templatePath,
            name,
            "component",
            Collections.emptyMap(),
            Collections.emptySet(),
            Collections.emptyMap()
        );
    }

    /**
     * Fragment作成 - ファクトリメソッド（カテゴリ付き版）
     */
    public static Fragment of(SecureTemplatePath templatePath, FragmentName name, String category) {
        return new Fragment(
            templatePath,
            name,
            category,
            Collections.emptyMap(),
            Collections.emptySet(),
            Collections.emptyMap()
        );
    }

    // === ドメインメソッド ===

    /**
     * フラグメントの使用例を生成
     */
    public String generateUsageExample(StoryName storyName) {
        Story story = stories.get(storyName);
        if (story == null) {
            return generateBasicUsageExample();
        }

        StringBuilder usage = new StringBuilder();
        usage.append("<!-- Fragment Usage Example -->\n");
        usage.append("<div th:insert=\"~{")
             .append(templatePath.forFilePath())
             .append(" :: ")
             .append(name.getValue())
             .append("(");

        // パラメータ例を追加
        Map<String, Object> exampleParams = story.getParameters();
        if (!exampleParams.isEmpty()) {
            List<String> paramStrings = new ArrayList<>();
            exampleParams.forEach((key, value) -> {
                if (value instanceof String) {
                    paramStrings.add(key + "='" + value + "'");
                } else {
                    paramStrings.add(key + "=" + value);
                }
            });
            usage.append(String.join(", ", paramStrings));
        }

        usage.append(")}\"></div>");
        return usage.toString();
    }

    /**
     * 基本的な使用例を生成
     */
    private String generateBasicUsageExample() {
        return "<div th:insert=\"~{" + templatePath.forFilePath() + 
               " :: " + name.getValue() + "}\"></div>";
    }

    // === 取得メソッド ===

    /**
     * 指定したストーリーを取得
     */
    public Optional<Story> getStory(StoryName storyName) {
        return Optional.ofNullable(stories.get(storyName));
    }

    /**
     * フラグメントの一意ID を取得
     */
    public String getId() {
        return templatePath.forUrl() + "#" + name.getValue();
    }

    /**
     * フラグメントの説明を取得
     */
    public String getDescription() {
        Object description = metadata.get("description");
        return description instanceof String value ? value : "";
    }

    public SecureTemplatePath getTemplatePath() {
        return templatePath;
    }

    public FragmentName getName() {
        return name;
    }

    public Set<String> getRequiredParameters() {
        return requiredParameters; // 既に不変コレクションなのでそのまま返す
    }

    public Map<StoryName, Story> getStories() {
        return stories; // 既に不変コレクションなのでそのまま返す
    }

    public String getCategory() {
        return category;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fragment fragment)) return false;
        return Objects.equals(templatePath, fragment.templatePath) &&
               Objects.equals(name, fragment.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templatePath, name);
    }

    @Override
    public String toString() {
        return "Fragment{" +
                "id='" + getId() + '\'' +
                ", category=" + category +
                ", stories=" + stories.size() +
                '}';
    }

}
