# BlackBoxPro P/G/E Status

## Current Phase

- phase: `done`
- qa_mode: `plugin`
- current_sprint: `sprint-005`
- current_task: `bbp-sprint-005-mod1122-matrix-evidence`
- owner: `Shared`
- updated_at: `2026-05-04 21:26 +08:00`

## Next Legal Action

Sprint 5 QA now passes in `docs/workflow/qa/sprint-005-qa.md`: `cell-20`, `cell-21`, and `cell-22` all completed startup and smoke with cleanup clean. Next legal action: draft Sprint 6 contract for Germ real physical click enhancement. Keep multi-bot scenario orchestration as a separate contract and do not mix it into the active sprint.

## Current Decision

BlackBoxPro is out of Sprint 5 QA failure triage. `cell-21` was repaired by reprovisioning from the `cell-20` baseline and clearing a stale lease; all three mod1122 cells now have passing startup and smoke evidence.

## Sprint Roadmap

- Sprint 1: P/G/E workflow, HTTP relay truth source, Germ semantic boundary, and `run_test` coverage boundary. Done.
- Sprint 2: `cell-20..22` Forge 1.12.2 business-mod smoke and cleanup evidence. Done.
- Sprint 3: Forge `1.20.1` input action support, Forge `1.12.2` preservation, and minimum build/static/runtime verification. Done.
- Sprint 4: Modern `1.21.11` / `1.21.1` Fabric + NeoForge input action sync. Done for build/static acceptance; runtime smoke blocked/unverified without same-version endpoints.
- Sprint 5: `cell-20/21/22` Forge 1.12.2 business-mod matrix evidence. Done.
- Sprint 6: Germ real physical click enhancement. Planned.
- Sprint 7: multi-bot scenario orchestrator. Planned as separate contract.

## Out Of Scope For This Round

- multi-bot scenario orchestration
- mineflayer-style high-level bot behavior ecosystem
- transport rewrite
- new `/actions` HTTP route
