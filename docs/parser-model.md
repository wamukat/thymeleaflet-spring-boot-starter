# Fragment Parser Output Model (Draft v1)

This document defines the parser output model planned in Issue #49.

## 1. Design Goals

- Keep parser output immutable and UI-friendly.
- Preserve declaration order and source location for diagnostics.
- Represent `missing` vs `empty string` explicitly (for story/render UI).

## 2. Proposed Core Types

## `FragmentDeclaration`

- `templatePath: String`
- `fragmentName: String`
- `parameters: List<FragmentParameter>`
- `originalDefinition: String`
- `location: SourceLocation`
- `diagnostics: List<FragmentDiagnostic>`

## `FragmentParameter`

- `name: String`
- `order: int` (declaration order)
- `status: ParameterStatus`  
  for parser step this is normally `DECLARED`; UI mapping can derive `MISSING`/`EMPTY_STRING`/`SET`
- `source: ParameterSource` (`SIGNATURE`, `JAVADOC`, `STORY`)

## `FragmentDiagnostic`

- `code: DiagnosticCode`
- `severity: DiagnosticSeverity`
- `message: String`
- `location: SourceLocation`

## `SourceLocation`

- `line: int`
- `column: int`

## 3. Enums

## `DiagnosticCode`

- `INVALID_SIGNATURE`
- `UNSUPPORTED_SYNTAX`
- `DUPLICATE_PARAMETER`
- `UNKNOWN`

## `DiagnosticSeverity`

- `ERROR`
- `WARNING`
- `INFO`

## `ParameterStatus` (UI側で使用)

- `DECLARED`
- `MISSING`
- `EMPTY_STRING`
- `SET`

## `ParameterSource`

- `SIGNATURE`
- `JAVADOC`
- `STORY`

## 4. Merge Policy (Parser + JavaDoc + Story)

1. Base order comes from parser declaration (`SIGNATURE`).
2. JavaDoc metadata is merged by parameter name.
3. Story value status is layered at runtime:
   - key missing -> `MISSING`
   - key exists, value empty string -> `EMPTY_STRING`
   - otherwise -> `SET`
4. Parameters not in signature but present in JavaDoc/story are appended with warning.

## 5. Backward Compatibility Strategy

- Current `FragmentInfo.parameters: List<String>` can be adapted from new model.
- Existing UI endpoints continue returning old shape first, then migrate in #53.
- Old regex path can coexist during transitional phase (#50-#53), removed in #54.
