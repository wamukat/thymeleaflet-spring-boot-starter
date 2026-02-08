# Agent Notes

## Sample App Reflection
- スターターをビルドした後（`mvn -DskipTests install`）は、サンプルアプリを再起動しないと変更が反映されない。
  - 例: `sample` ディレクトリで `mvn spring-boot:run`

## E2E Testing
- 実装後は必ずE2Eテストを実施する。
  - 例: `npm run test:e2e`
- リリース前にも必ずE2Eテストを実施する（必須）。

## PR/Issue Notes
- PR本文に「どのIssueをクローズするか（例: `Closes #123`）」を明記する。
- Issue作成時は、リリースノート整理のため `type:*` ラベルを必ず付与する。
  - 基本: `type:feature` / `type:fix` / `type:refactor` / `type:docs` / `type:test` / `type:build` / `type:chore`

## PR/Issue Formatting
- PR/Issue本文は「`\n` の文字列」ではなく、実際の改行（LF）で記載する。
- 改行付きで確実に登録する方法（例）:
  - PR: `cat > /tmp/pr_body.md <<'EOF' ... EOF` → `gh pr edit <PR番号> --body-file /tmp/pr_body.md`
  - Issue: `cat > /tmp/issue_body.md <<'EOF' ... EOF` → `gh issue create --title "..." --body-file /tmp/issue_body.md`
  - Release: `cat > /tmp/release.md <<'EOF' ... EOF` → `gh release edit <tag> --notes-file /tmp/release.md`

## Release Flow
- 「新しいバージョンとしてリリースする」と依頼されたら、現在のバージョン番号を添えて「バージョン番号はどうする？」と確認する。
- リリース作業は `main` へ直接コミットせず、`release/x.y.z` ブランチを作成して進める。
- `release/x.y.z` では `pom.xml` と `sample/pom.xml` のバージョン更新、`CHANGELOG.md` 更新を行う。
- 更新後は PR を作成し、CI/E2E確認後に `main` へマージしてからタグ作成・GitHub Release・デプロイを行う。
- 確定後は `RELEASE.md` の手順に従ってリリースを進める。
- リリース後にデモデプロイを実施する場合は、`Dockerfile` の Java バージョンとコンパイル要件が一致していることを確認する。

## Changelog Format
- `CHANGELOG.md` は Keep a Changelog 形式で、各バージョンをカテゴリ別に記載する。
- 基本カテゴリは `Added` / `Changed` / `Fixed` / `Removed` / `Refactored` / `Docs` / `Build` / `Test`（必要なものだけ使用）。
- 「Issue一覧だけ」にはせず、まずカテゴリごとにユーザー影響を記載し、必要に応じて末尾に Issue 番号を併記する。

## Branch Cleanup
- 「ブランチを整理してください」と依頼されたら、未マージのブランチ有無を確認する。
- マージ済みブランチは削除し、未マージがあればブランチ名を報告する。

## Branching Rule
- ソース修正を始める前に、必ず作業用ブランチを作成してから作業する。
- 作業が切り替わる指示を受けたら、`main` に戻ってから指示内容に応じたブランチを作成する。

## Browser Tooling
- ブラウザ確認・操作は `agent-browser` を優先して利用する。

## Build Environment
- ライブラリ利用時の要件は Java 17+。
- このリポジトリをソースからビルドする場合は Java 21+（Error Prone / NullAway要件）。
- Dockerイメージも Java 21系を利用する（build/runtime ともに揃える）。

## Null Safety (NullAway)
- `null` で状態を表現しない（可能な限り非null設計を優先する）。
- `@Nullable` を付ける前に、以下を先に検討する:
  - 成功/失敗を型で分離する（例: sealed interface, Result型）
  - 値の有無は `Optional` で表現する
  - 未初期化状態は sentinel / Null Object パターンで表現する
  - コレクションは空コレクションを基本とする
- `@Nullable` は外部仕様上やむを得ない境界でのみ使い、理由をコード上で説明できる状態にする。
- nullable な設定値は、境界（例: `Properties` → 解決済み設定オブジェクト変換）で正規化し、内部処理は非null前提に寄せる。

## Docs Screenshot Workflow
- キャプチャは Playwright を使って取得する（`npx -p playwright` で依存を一時的に利用）。
- 目安の設定:
  - ビューポート: `1440x900`
  - `deviceScaleFactor: 2` で高解像度にする。
- 画面遷移:
  - 該当フラグメントをクリックして選択。
  - ストーリー切り替えはストーリーリンクをクリック。
  - ビューポート切り替えは `<select>` の `option[value=...]` を `selectOption` で変更。
- 取得対象:
  - 全体: `page.screenshot`
  - カード: `.card-thymeleaflet` を `h3` 見出しテキストで絞り込み、`locator.screenshot`。
  - プレビューは `Preview` カードを撮影。
  - フルスクリーンは `Fullscreen` ボタン → `.preview-fullscreen` を撮影。
