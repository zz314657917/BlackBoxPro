# Task Contract: bbp-sprint-001-workflow-truth-source

## Task ID

`bbp-sprint-001-workflow-truth-source`

## Role

Generator

## Goal

Establish the BlackBoxPro P/G/E workflow skeleton and make the current stable-mainline plan reviewable as a Sprint 1 contract.

This task only changes workflow and knowledge documents. It must not change source code, Gradle configuration, test-cell scripts, or global `.codex` files.

## Success Criteria

- `docs/workflow/status.md` exists and states current phase, current sprint, current task, QA mode, and next legal action.
- `docs/workflow/spec.md` exists and covers the 3 Sprint stable-mainline direction.
- `docs/workflow/tasks/sprint-001.md` exists and contains all required P/G/E contract sections.
- `docs/workflow/main-log.md` records the planning event.
- Documentation states that HTTP relay is the current production chain and Plugin Message Channel is historical context.
- Documentation states that `germ_gui_part_dos` is Germ YAML / `clickDos` semantic execution, not physical click validation.
- Documentation states that multi-bot orchestration is out of scope for this round and not implemented.

## Allowed Paths

- `docs/workflow/**`
- `knowledge/tasks/current-task.md`
- `knowledge/tasks/timeline.md`
- `knowledge/05-current-focus.md`

## Denied Paths

- `common/**`
- `mod/**`
- `plugin/**`
- `scripts/test-cells/**`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `C:/Users/Administrator/.codex/**`

## Constraints

- Only documentation and workflow handoff files may be changed.
- Do not modify source code.
- Do not run commands that start Minecraft, test-cell, server, client, or proxy processes.
- Do not write secrets, tokens, passwords, or private server addresses.
- Do not describe `germ_gui_part_dos` as real physical click success.
- Do not claim multi-bot orchestration is implemented.
- If existing workflow files are present, merge minimally instead of overwriting.

## Acceptance Commands

```powershell
Test-Path "docs/workflow/status.md"
Test-Path "docs/workflow/spec.md"
Test-Path "docs/workflow/tasks/sprint-001.md"
rg -n "HTTP relay|germ_gui_part_dos|多 bot|multi" "docs/workflow" "knowledge"
git diff --check
```

## Output

The worker report must include:

- changed files
- purpose of each workflow file
- confirmation that no source code, Gradle, test-cell script, or global `.codex` file was changed
- executed acceptance commands and results
- explicit note that no build or runtime test was run because this task is documentation-only
- next step: Evaluator reviews and either approves or rewrites this contract

## Stop Rules

- Stop if the repository is not a git repo.
- Stop if satisfying the task requires source-code changes.
- Stop if existing workflow files conflict with this contract.
- Stop if `knowledge/tasks/current-task.md` contradicts the stable-mainline plan.
- Stop if any required contract section cannot be completed with the available repository context.
