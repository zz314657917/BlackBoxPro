# BlackBoxPro P/G/E Status

## Current Phase

- phase: `fix`
- qa_mode: `plugin`
- current_sprint: `sprint-006`
- current_task: `bbp-sprint-006-germ-real-physical-click`
- owner: `Generator`
- updated_at: `2026-05-05 16:40 +08:00`

## Next Legal Action

Sprint 6 QA still fails in `docs/workflow/qa/sprint-006-qa.md`. Next legal action: either discover a stable Germ screen/event queue hook, or rewrite the Sprint 6 contract around a test Germ page with an observable click side effect.

## Current Decision

BlackBoxPro Sprint 6 has implementation/build evidence, but runtime acceptance still fails. `click_germ_component` can now invoke the obfuscated component method `ALLATORIxDEMO(float,float):void` with screen-coordinate evidence, but the real Lmshop Germ page did not produce a business side effect: PlayerPoints stayed `7988 -> 7988`. `germ_gui_part_dos` remains semantic execution only and cannot be used as physical-click PASS evidence.

## Sprint Roadmap

- Sprint 1: P/G/E workflow, HTTP relay truth source, Germ semantic boundary, and `run_test` coverage boundary. Done.
- Sprint 2: `cell-20..22` Forge 1.12.2 business-mod smoke and cleanup evidence. Done.
- Sprint 3: Forge `1.20.1` input action support, Forge `1.12.2` preservation, and minimum build/static/runtime verification. Done.
- Sprint 4: Modern `1.21.11` / `1.21.1` Fabric + NeoForge input action sync. Done for build/static acceptance; runtime smoke blocked/unverified without same-version endpoints.
- Sprint 5: `cell-20/21/22` Forge 1.12.2 business-mod matrix evidence. Done.
- Sprint 6: Germ real physical click enhancement. QA failed; fix required.
- Sprint 7: multi-bot scenario orchestrator. Planned as separate contract.

## Out Of Scope For This Round

- multi-bot scenario orchestration
- `germ_gui_part_dos execute=true` as physical-click proof
- mineflayer-style high-level bot behavior ecosystem
- transport rewrite
- new `/actions` HTTP route
