# BlackBoxPro P/G/E Status

## Current Phase

- phase: `contract-approved`
- qa_mode: `plugin`
- current_sprint: `sprint-005`
- current_task: `bbp-sprint-005-mod1122-matrix-evidence`
- owner: `Evaluator`
- updated_at: `2026-05-04 18:10 +08:00`

## Next Legal Action

Sprint 5 contract is drafted in `docs/workflow/tasks/sprint-005.md` and approved in `docs/workflow/reviews/sprint-005-contract-review.md`.

Next legal action: run the approved Sprint 5 matrix acceptance commands directly, or invoke a bounded test-only worker that writes `docs/workflow/worker-results/bbp-sprint-005-mod1122-matrix-evidence-result.md`.

## Current Decision

BlackBoxPro is entering Sprint 5 runtime matrix validation for the Forge 1.12.2 business-mod pool. The sprint must prove fixed CloudStorage `startup` and `smoke` on `cell-20`, `cell-21`, and `cell-22`, with cleanup evidence for every cell.

## Sprint Roadmap

- Sprint 1: P/G/E workflow, HTTP relay truth source, Germ semantic boundary, and `run_test` coverage boundary. Done.
- Sprint 2: `cell-20..22` Forge 1.12.2 business-mod smoke and cleanup evidence. Done with `cell-20` CloudStorage startup/smoke evidence.
- Sprint 3: Forge `1.20.1` input action support, Forge `1.12.2` preservation, and minimum build/static/runtime verification. Done.
- Sprint 4: Modern `1.21.11` / `1.21.1` Fabric + NeoForge input action sync. Done for build/static acceptance; runtime smoke blocked/unverified without same-version endpoints.
- Sprint 5: `cell-20/21/22` Forge 1.12.2 business-mod matrix evidence. Contract approved.
- Sprint 6: Germ real physical click enhancement. Planned.
- Sprint 7: multi-bot scenario orchestrator. Planned as separate contract.

## Out Of Scope For This Round

- multi-bot scenario orchestration
- mineflayer-style high-level bot behavior ecosystem
- transport rewrite
- new `/actions` HTTP route
