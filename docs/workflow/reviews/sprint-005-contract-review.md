# Sprint 5 Contract Review

## Verdict

`APPROVED`

## Reviewed Contract

- `docs/workflow/tasks/sprint-005.md`
- Task ID: `bbp-sprint-005-mod1122-matrix-evidence`

## Findings

- Required contract fields are present: `Task ID`, `Role`, `Goal`, `Success Criteria`, `Allowed Paths`, `Denied Paths`, `Runtime Inputs`, `Acceptance Commands`, `Constraints`, `Output`, `Stop Rules`.
- Scope is narrow enough for a test-only worker or Codex direct QA: run `startup` and `smoke` for fixed CloudStorage jar on `cell-20`, `cell-21`, and `cell-22`.
- The contract closes the Sprint 2 residual risk by requiring all three business-mod cells, not only whichever cell is acquired first.
- Denied paths protect source code, Gradle, test-cell scripts, CloudStorage, and global `.codex`.
- The runtime commands use explicit `-CellId` with `-AcquireCell`, avoiding accidental proof from only the default cell.
- Cleanup gates include per-run stop/release and final port/process checks.
- Acceptance commands avoid using repo-external CloudStorage paths as current-repo `git diff` pathspecs; CloudStorage diff is checked with a separate `git -C` command when that directory is a git repo.

## Decision

Contract approved for test-only execution.

Next legal action: run the approved Sprint 5 matrix acceptance commands directly or invoke a bounded test worker that writes `docs/workflow/worker-results/bbp-sprint-005-mod1122-matrix-evidence-result.md`.
