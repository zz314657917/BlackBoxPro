# BlackBoxPro P/G/E Status

## Current Phase

- phase: `done`
- qa_mode: `plugin`
- current_sprint: `sprint-004`
- current_task: `bbp-sprint-004-modern-input-sync`
- owner: `Evaluator`
- updated_at: `2026-05-04 17:45 +08:00`

## Next Legal Action

Sprint 4 implementation and QA are recorded in `docs/workflow/qa/sprint-004-qa.md`.

Next legal action: draft Sprint 5 contract for `cell-20/21/22` Forge 1.12.2 business-mod matrix evidence, or perform branch/local artifact cleanup before the next sprint.

## Current Decision

BlackBoxPro completed the modern `1.21.11` / `1.21.1` Fabric + NeoForge input action sync for build/static acceptance. Runtime smoke remains unverified for `1.21.x` because no real same-version endpoint was available during acceptance.

## Sprint Roadmap

- Sprint 1: P/G/E workflow, HTTP relay truth source, Germ semantic boundary, and `run_test` coverage boundary. Done.
- Sprint 2: `cell-20..22` Forge 1.12.2 business-mod smoke and cleanup evidence. Done with `cell-20` CloudStorage startup/smoke evidence.
- Sprint 3: Forge `1.20.1` input action support, Forge `1.12.2` preservation, and minimum build/static/runtime verification. Done.
- Sprint 4: Modern `1.21.11` / `1.21.1` Fabric + NeoForge input action sync. Done for build/static acceptance; runtime smoke blocked/unverified without same-version endpoints.
- Sprint 5: `cell-20/21/22` Forge 1.12.2 business-mod matrix evidence. Planned.
- Sprint 6: Germ real physical click enhancement. Planned.
- Sprint 7: multi-bot scenario orchestrator. Planned as separate contract.

## Out Of Scope For This Round

- multi-bot scenario orchestration
- mineflayer-style high-level bot behavior ecosystem
- transport rewrite
- new `/actions` HTTP route
