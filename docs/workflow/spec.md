# BlackBoxPro Stable QA Roadmap

## Summary

This workflow makes BlackBoxPro a more reliable real-client Minecraft QA foundation.

The current production chain is HTTP relay:

`BlackBoxApi -> ModRelayClient -> Mod HTTP /execute`

Plugin Message Channel descriptions are historical context only.

## Sprint 1: Workflow And Truth Source

Goal: establish P/G/E workflow files and align project truth sources before any source-code change.

Expected outcomes:

- `docs/workflow/status.md` is the P/G/E phase truth source.
- `docs/workflow/tasks/sprint-001.md` is reviewable before worker execution.
- `ActionCatalog` remains the action definition truth source.
- Germ boundaries are explicit:
  - `query_germ_screen` and `query_germ_hit_test` are read-only probes.
  - `germ_gui_part_dos` executes Germ YAML / `clickDos` semantics when `execute=true`.
  - `germ_gui_part_dos` is not proof of real physical clicking.
  - `click_screen_at` and `click_germ_component` are the physical/client event validation path.
- `run_test` default coverage excludes actions that require a real Germ page, client session switching, or business-mod environment.

## Sprint 2: 1.12.2 Test-Cell Runtime Evidence

Goal: turn the existing 1.12.2 test-cell pools into repeatable runtime evidence.

Expected outcomes:

- `cell-01..05` remains the normal 1.12.2 plugin, Germ, and BC regression pool.
- `cell-20..22` remains the Forge 1.12.2 business-mod pool.
- `Run-TestCellMod1122Regression.ps1` has a documented startup/smoke evidence path.
- At least one real Forge 1.12.2 business mod jar has a reproducible startup or smoke record.
- Cleanup is a hard acceptance rule: stop/release lease, no related listening ports, no matching `cmd/java/javaw` leftovers.

## Sprint 3: Priority Client Input Support

Goal: prioritize the currently useful client lines by bringing Forge `1.20.1` to parity with the already validated Forge `1.12.2` input actions.

Expected outcomes:

- Forge `1.20.1` action registration and implementation files align with `ActionCatalog`.
- Forge `1.12.2` remains the preserved baseline for the same target actions.
- Plugin API wrappers, parameter metadata, and test catalog remain aligned with `ActionCatalog`.
- Priority input actions:
  - `move_mouse`
  - `click_mouse`
  - `click_screen_at`
  - `query_cursor_state`
  - `key_press`
  - `type_text`
- Verification includes common tests, priority-version builds, static action search, and runtime smoke only when a real same-version endpoint is available.
- `1.21.11` / `1.21.1` input action implementation is deferred to a later sprint.

## Multi-Bot Boundary

Multi-bot orchestration is not part of this roadmap.

The current architecture remains one real Minecraft client process per player identity. Future multi-bot support should be a repo-side scenario orchestrator, for example:

`scenario.yml/json -> acquire multiple cells -> dispatch actions per bot endpoint -> aggregate evidence -> cleanup`

That future tool should not be represented as already implemented in this workflow.
