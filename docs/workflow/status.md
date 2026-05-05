# BlackBoxPro P/G/E Status

## Current Phase

- phase: `done`
- qa_mode: `plugin`
- current_sprint: `sprint-006`
- current_task: `bbp-sprint-006-germ-real-physical-click`
- owner: `Evaluator`
- updated_at: `2026-05-05 19:27 +08:00`

## Next Legal Action

Sprint 6 QA passes in `docs/workflow/qa/sprint-006-qa.md`. Next legal action: draft Sprint 7 multi-bot scenario orchestrator contract, or open a separate contract for further Germ/Lmshop production-page hardening.

## Current Decision

BlackBoxPro Sprint 6 is done. The real Lmshop page remains diagnostic failure evidence because callable Germ hooks did not change PlayerPoints, but the approved fixed Germ page produced deterministic runtime evidence: `click_germ_component` with `screenClickPolicy=always` returned `clickPath=component+screen`, hit the fixed button bounds at `x=214,y=113`, and changed `query_chat_history` from `beforeMarkerCount=0` to `afterMarkerCount=1` with `<zzzderk> BBP_GERM_FIXED_CLICK_MARKER`. `germ_gui_part_dos` remains semantic execution only and cannot be used as physical-click PASS evidence.

## Sprint Roadmap

- Sprint 1: P/G/E workflow, HTTP relay truth source, Germ semantic boundary, and `run_test` coverage boundary. Done.
- Sprint 2: `cell-20..22` Forge 1.12.2 business-mod smoke and cleanup evidence. Done.
- Sprint 3: Forge `1.20.1` input action support, Forge `1.12.2` preservation, and minimum build/static/runtime verification. Done.
- Sprint 4: Modern `1.21.11` / `1.21.1` Fabric + NeoForge input action sync. Done for build/static acceptance; runtime smoke blocked/unverified without same-version endpoints.
- Sprint 5: `cell-20/21/22` Forge 1.12.2 business-mod matrix evidence. Done.
- Sprint 6: Germ real physical click enhancement. Done with fixed Germ test page evidence.
- Sprint 7: multi-bot scenario orchestrator. Planned as separate contract.

## Out Of Scope For This Round

- multi-bot scenario orchestration
- `germ_gui_part_dos execute=true` as physical-click proof
- mineflayer-style high-level bot behavior ecosystem
- transport rewrite
- new `/actions` HTTP route
