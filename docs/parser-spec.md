# Thymeleaf Fragment Parser Spec (Draft v1)

This document defines the target behavior for the new fragment parser (Epic #39).
Scope is **fragment declaration parsing** (`th:fragment="..."`) and normalization used by discovery/UI.

## 1. Goals

- Remove regex-only extraction for fragment signatures.
- Support practical Thymeleaf fragment declaration syntax in a deterministic way.
- Produce stable parser output for UI, Story values ordering, and diagnostics.

## 2. Parsing Scope

### 2.1 Input Scope

- Target files: HTML templates discovered under configured template paths.
- Target attribute: `th:fragment`.
- Attribute quoting: single and double quote are supported.
- Attribute location: any element.

### 2.2 Output Scope

Parser output for one declaration consists of:

- `fragmentName`: normalized fragment name
- `parameters`: ordered parameter list
- `originalDefinition`: raw `th:fragment` value
- `diagnostics`: parse warnings/errors with location

## 3. Supported Grammar (v1)

The following signature form is supported:

```ebnf
signature      = fragmentName , [ ws , "(" , ws , [ parameterList ] , ws , ")" ] ;
parameterList  = parameter , { ws , "," , ws , parameter } ;
parameter      = identifier ;
fragmentName   = identifier ;
identifier     = firstChar , { nextChar } ;
firstChar      = "A".."Z" | "a".."z" | "0".."9" ;
nextChar       = firstChar | "_" | "-" ;
ws             = { " " | "\t" | "\n" | "\r" } ;
```

### 3.1 Normalization Rules

- Leading/trailing whitespace is ignored.
- Parameter order is preserved as declared.
- `fragmentName()` is valid and yields empty parameter list.
- Duplicate parameter names are preserved (reported as warning, not auto-fixed).

## 4. Explicitly Not Supported (v1)

The parser does **not** support these declaration forms in v1:

- Fragment selector forms beyond identifier name (e.g. CSS selector style fragment names).
- Parameter default expression syntax inside declaration.
- Parameter assignment syntax inside declaration.
- Nested parenthesis or expression parsing inside parameter tokens.
- Non-identifier parameter tokens.

When unsupported syntax is detected, parser behavior:

- emit diagnostic with reason (`UNSUPPORTED_SYNTAX`)
- skip creating normalized declaration for that entry
- continue processing other declarations

## 5. Ambiguity Resolution Rules

- If parentheses are present but unbalanced, treat as parse error (`INVALID_SIGNATURE`).
- If comma-separated token is empty (e.g. `a,,b`), treat as parse error.
- If identifier validation fails, treat as parse error.
- If multiple `th:fragment` declarations are present in one template, all valid ones are returned in appearance order.

## 6. Example Matrix

| Input `th:fragment` value | Result |
| --- | --- |
| `profileCard` | name=`profileCard`, params=`[]` |
| `profileCard()` | name=`profileCard`, params=`[]` |
| `profileCard(name, age)` | name=`profileCard`, params=`[name, age]` |
| ` profileCard ( name , age ) ` | name=`profileCard`, params=`[name, age]` |
| `profileCard(name,,age)` | error `INVALID_SIGNATURE` |
| `profileCard(name` | error `INVALID_SIGNATURE` |
| `profileCard(name='x')` | error `UNSUPPORTED_SYNTAX` |

## 7. Compatibility Policy

- Existing simple signatures remain compatible.
- Currently accepted-but-ambiguous strings may become explicit errors with diagnostics.
- UI should surface warnings/errors without failing the full fragment list.

## 8. Test Requirements (for implementation issues)

- Unit tests for grammar acceptance/rejection.
- Golden tests for normalization output ordering.
- Regression tests for current sample templates.
- Error diagnostics tests for unsupported/invalid signatures.
