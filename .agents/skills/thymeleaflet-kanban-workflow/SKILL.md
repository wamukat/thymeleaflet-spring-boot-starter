---
name: thymeleaflet-kanban-workflow
description: Thymeleaflet project Kanban execution workflow. Use when working on /Users/takuma/workspace/thymeleaflet-spring-boot-starter tasks that involve the local Kanbalone board, adding improvement tickets, implementing tickets, reviewing with sub-agents, moving cards across lanes, or reporting ticket progress.
---

# Thymeleaflet Kanban Workflow

## Core Rule

Treat Kanban lane movement and resolution as separate actions.

- Move a ticket to `done` only after implementation, verification, and sub-agent review have no findings.
- When moving a ticket to `done`, keep `isResolved: false`.
- Never set `isResolved: true`; the user reviews cards in `done` and resolves them manually.

## Required Skills

Use this skill together with:

- `kanbalone-api` for all Kanban operations.
- `thymeleaflet-test-execution` for Maven/sample/E2E verification.

Use the Kanbalone HTTP API only. Do not operate the Kanban board through browser clicks unless the user explicitly asks for browser UI work.

## Workflow

1. Inspect context.
   - Confirm the target board URL or derive it from the user request.
   - Read the board to resolve lane IDs and existing tickets.
   - Check `git status -sb`.

2. Create tickets before implementation when the user asks to add tasks.
   - Add focused tickets to `todo`.
   - Prefer small tickets that can be implemented, verified, and reviewed independently.
   - Add a clear goal and acceptance checklist to each ticket.
   - Use the ticket creation payload shape shown in `kanbalone-api`: `laneId`, `title`, `bodyMarkdown`, `priority`, `tagIds`, `blockerIds`, `parentTicketId`, `isResolved:false`, and `isArchived:false`.

3. Start work on one ticket at a time.
   - Follow repository rules: switch from `main` and create a work branch before source edits.
   - For multiple tickets, do not start the next ticket's source edits until the current ticket has completed implementation, verification, and no-findings review. If the user wants one branch for several small tickets, keep commits/comments clearly separated by ticket.
   - Move the ticket to `doing` with `isResolved:false`.
   - Add a start comment with scope and branch.

4. Implement and verify.
   - Keep changes scoped to the active ticket.
   - Run the relevant verification.
   - For this repository, implementation work requires E2E unless there is a concrete blocker.
   - A sample server that has not been started is a setup gap, not a blocker. Start/restart the sample app and run E2E before done.
   - Treat E2E as blocked only when the environment prevents it after a concrete attempt. Record the command and failure.
   - Add an implementation comment with changed scope and verification commands.

5. Sub-agent review gate.
   - Ask a sub-agent to review the implemented changes before moving the ticket to `done`.
   - The review prompt must ask for findings first, with file/line references, or an explicit "no findings".
   - The review prompt must also mention the ticket IDs and require checking Kanban workflow compliance: no premature done movement and no `isResolved:true`.
   - Acceptable review output is either `No findings` or a findings list with file/line references and concrete required changes.
   - If the review finds issues, fix them and request another review.
   - Do not move the ticket to `done` until review has no findings.

6. Move to done without resolving.
   - Use transition payload with `laneName:"done"` and `isResolved:false`.
   - Add a comment that review had no findings and the ticket is ready for the user's manual resolve.
   - Do not call any update/transition that sets `isResolved:true`.
   - `review-pending` is not assumed to be a Kanban lane. Represent review-pending state by keeping the ticket in `doing` and adding a status comment unless the board explicitly has a review lane.

## API Patterns

Move to doing:

```bash
python3 ~/.codex/skills/kanbalone-api/scripts/kanbalone_api.py --base "$BASE" \
  PATCH /api/tickets/"$TICKET_ID"/transition \
  '{"laneName":"doing","isResolved":false}'
```

Create a ticket:

```bash
python3 ~/.codex/skills/kanbalone-api/scripts/kanbalone_api.py --base "$BASE" \
  POST /api/boards/"$BOARD_ID"/tickets \
  '{"laneId":31,"title":"<title>","bodyMarkdown":"# Goal\n\n<goal>\n\n## Acceptance\n\n- [ ] <item>","priority":2,"tagIds":[],"blockerIds":[],"parentTicketId":null,"isResolved":false,"isArchived":false}'
```

Add a comment:

```bash
python3 ~/.codex/skills/kanbalone-api/scripts/kanbalone_api.py --base "$BASE" \
  POST /api/tickets/"$TICKET_ID"/comments \
  '{"bodyMarkdown":"<markdown body>"}'
```

Move to done, still unresolved:

```bash
python3 ~/.codex/skills/kanbalone-api/scripts/kanbalone_api.py --base "$BASE" \
  PATCH /api/tickets/"$TICKET_ID"/transition \
  '{"laneName":"done","isResolved":false}'
```

If a ticket was mistakenly resolved, immediately move/update it back to unresolved if the API allows it, and add a correction comment. Prefer:

```bash
python3 ~/.codex/skills/kanbalone-api/scripts/kanbalone_api.py --base "$BASE" \
  PATCH /api/tickets/"$TICKET_ID"/transition \
  '{"laneName":"doing","isResolved":false}'
```

If transition cannot update resolution, try a ticket patch with `{"isResolved":false}` if supported by the local API. Do not delete, archive, or rewrite the erroneous history.

## Review Prompt Template

Use a fresh sub-agent for each review cycle:

```text
Review the current uncommitted changes in <repo> for Kanban ticket(s) <ids>.
Do not edit files. Findings first, ordered by severity, with file/line references.
If there are no findings, say "No findings" clearly.

Also check workflow compliance:
- no ticket should move to done before no-findings review
- no API payload or requested transition should set isResolved:true
- E2E must be run before done unless there is a concrete recorded blocker

Mention residual test gaps.
```

## Comments

Start comment:

```text
Started work. Scope: <scope>. Branch: `<branch>`.
```

Implementation comment:

```text
Implemented. <short summary>.

Verification:
- `<command>` passed
- `<command>` passed

Status: review-pending; will not move to done until sub-agent review has no findings.
```

Ready-for-user comment:

```text
Sub-agent review returned no findings.

Moved to `done` with `isResolved:false` for user review and manual resolve.
```

Correction comment:

```text
Correction: moved back from done/resolved state. This ticket remains review-pending until sub-agent review has no findings.
```
