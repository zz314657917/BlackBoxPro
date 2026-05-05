# Sprint 4 Contract Review

## Verdict

`APPROVED`

## Reviewed Contract

- `docs/workflow/tasks/sprint-004.md`
- Task ID: `bbp-sprint-004-modern-input-sync`

## Findings

- Required contract fields are present: `Task ID`, `Role`, `Goal`, `Success Criteria`, `Allowed Paths`, `Denied Paths`, `Constraints`, `Acceptance Commands`, `Output`, `Stop Rules`.
- Scope is narrow enough for Generator execution: modern `1.21.11` / `1.21.1` Fabric + NeoForge input action sync only.
- Denied paths protect known-good priority baselines: `1.20.1`, `1.12.2`, `ActionCatalog`, plugin source, test-cell scripts, Gradle config, and global `.codex`.
- Acceptance commands include static search, common tests, modern build tasks, denied-path diff, whitespace check, and status output.
- Runtime PASS is correctly gated on real same-version endpoints; no lower-version runtime evidence can substitute for `1.21.x`.

## Decision

Contract approved for implementation or test-worker preparation.

Next legal action: Generator implements Sprint 4 within the approved paths, or Codex invokes a bounded worker using this contract.
