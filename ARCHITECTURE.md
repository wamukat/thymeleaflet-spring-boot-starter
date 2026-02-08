# Architecture Rules

このドキュメントは、現在このリポジトリで採用しているアーキテクチャ制約を明文化したものです。  
実際の自動検証は `src/test/java/io/github/wamukat/thymeleaflet/architecture/ArchitectureConstraintArchTest.java` で実施します。

## 1. レイヤ構成

- `domain`: ビジネス概念・ドメインロジック
- `application`: ユースケース、ポート（inbound/outbound）
- `infrastructure`: フレームワーク、I/O、Web、設定、adapter実装

## 2. 強制ルール（ArchUnitで検証）

1. `application.port.inbound` / `application.port.outbound` のトップレベルクラスは interface であること
2. `StorybookProperties` への依存は `infrastructure.configuration` に限定すること
3. `domain` は `application` / `infrastructure` に依存しないこと
4. `domain.model` / `domain.model.configuration` は Spring / Servlet に依存しないこと
5. 以下の outbound port 実装は `infrastructure.adapter..` または `infrastructure.web..` に配置すること
   - `StoryDataPort`
   - `DocumentationAnalysisPort`
   - `FragmentCatalogPort`
   - `StoryPresentationPort`
   - `JavaDocLookupPort`
   - `FragmentDependencyPort`
6. `application.port.inbound` / `application.port.outbound` は `infrastructure` に依存しないこと
7. `domain` は Spring stereotype（`@Component`, `@Service`, `@Repository`, `@Controller`）を付けないこと

## 3. 運用ルール（開発体験とのバランス）

- `application.service` から `infrastructure` への依存は「原則避ける」が、現時点では一律禁止しない
- UI協調処理（`Model` 連携など）は、過剰な抽象化で複雑化させない
- ただし、新規機能では可能な範囲で port 経由に寄せる

## 4. 変更手順

1. 制約を変える場合は、先にこのドキュメントを更新する
2. 次に `ArchitectureConstraintArchTest` を更新する
3. 最後に実装を合わせ、`mvn test` と `npm run test:e2e` を通す

