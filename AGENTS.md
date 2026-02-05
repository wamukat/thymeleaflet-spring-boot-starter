# Agent Notes

## Sample App Reflection
- スターターをビルドした後（`mvn -DskipTests install`）は、サンプルアプリを再起動しないと変更が反映されない。
  - 例: `sample` ディレクトリで `mvn spring-boot:run`

## PR/Issue Notes
- PR本文に「どのIssueをクローズするか（例: `Closes #123`）」を明記する。

## PR/Issue Formatting
- PR/Issue本文に書く改行は `\n`（LF）であることを確認する。
- 改行付きで確実に登録する方法（例）:
  - PR: `gh pr edit <PR番号> --body-file /tmp/pr_body.md`
  - Issue: `gh issue create --title "..." --body-file /tmp/issue_body.md`

## Release Flow
- 「新しいバージョンとしてリリースする」と依頼されたら、現在のバージョン番号を添えて「バージョン番号はどうする？」と確認する。
- 確定後は `RELEASE.md` の手順に従い、`pom.xml` と `sample/pom.xml` のバージョンを更新し、`CHANGELOG.md` を更新してリリースを進める。
