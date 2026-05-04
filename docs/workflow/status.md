# BlackBoxPro P/G/E Status

## Current Phase

- phase: `done`
- qa_mode: `plugin`
- current_sprint: `sprint-003`
- current_task: `bbp-sprint-003-priority-input-support`
- owner: `Evaluator`
- updated_at: `2026-05-04 06:04 +08:00`

## Next Legal Action

Sprint 3 build/static QA and priority-version runtime smoke are complete in `docs/workflow/qa/sprint-003-qa.md`.

Next legal action: open the next sprint for deferred `1.21.x` input sync, or separately plan multi-bot orchestration.

## Current Decision

BlackBoxPro will prioritize the currently useful client lines first: Forge `1.20.1` and Forge `1.12.2`. The earlier `1.21.x` modern sync target is deferred to a later sprint.

## Sprint Roadmap

- Sprint 1: P/G/E workflow, HTTP relay truth source, Germ semantic boundary, and `run_test` coverage boundary. Done.
- Sprint 2: `cell-20..22` Forge 1.12.2 business-mod smoke and cleanup evidence. Done with `cell-20` CloudStorage startup/smoke evidence.
- Sprint 3: Forge `1.20.1` input action support, Forge `1.12.2` preservation, and minimum build/static/runtime verification. Done.

## Out Of Scope For This Round

- multi-bot scenario orchestration
- mineflayer-style high-level bot behavior ecosystem
- `1.21.11` / `1.21.1` input action implementation in this sprint
- transport rewrite
- new `/actions` HTTP route
