# Sequential location-reliability work

> Execution update, 27 October 2026: the user later authorized all software stages in one sustained implementation run. The original sequential prompts below remain available for future continuation; current completion and verification evidence live in STATUS.md. Physical-device trials remain separate.

For future bounded continuation, use the prompts in PROMPTS.md against this same checkout and select only unfinished work from STATUS.md. The software stages were subsequently implemented together under explicit user authorization; do not restart completed work from run 01.

## Shared context

- CONTEXT.md: goals, evidence, original inputs and important constraints.
- STATUS.md: current state and append-only handoffs. Every agent reads and updates it.
- RUNS.md: bounded scope, exclusions and acceptance for each run.
- ASSESSMENT.md: complete two-log analysis and code references at the original baseline.
- REFERENCE_PLAN.md: complete solution rationale. Its earlier broad delivery groups are superseded by RUNS.md for execution.
- PROMPTS.md: ready-to-paste prompts, one for each run.

The context is carried by these files and the current working tree. A fresh agent must read them; it must not assume access to the earlier conversation. These files are outside the ignored temporary analysis directory. They have been created locally but are not automatically committed or copied to another checkout. If moving to another checkout, transfer the plan documents and prior code changes together; original personal logs remain local.

## Workflow

1. Paste one selected prompt from PROMPTS.md into a fresh chat opened on D:/PROJECTS/vibe-navigator.
2. Review its diff, tests, open issues and STATUS.md handoff before starting the next chat.
3. If a run is incomplete, repeat that run's prompt; the agent resumes using its handoff. Do not skip an unmet code prerequisite.
4. Hardware-only checks may remain NEEDS_DEVICE when later code work does not depend on their results. Record those checks explicitly; never represent them as passed.
5. If scope grows, stop at a coherent, recorded checkpoint and propose a bounded follow-on run. Do not silently absorb later roadmap items.

No automatic commit, branch creation, installation, publishing, scheduling, new-chat creation or delegation is implied by this package. Follow repository guidance and any explicit instructions in the new chat.

This structure uses persistent context and per-phase records as described in [OpenAI's iterative workflow guidance](https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex).
