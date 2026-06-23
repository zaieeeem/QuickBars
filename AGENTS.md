# AGENTS.md — working agreement for AI agents in this homelab

Read this first. It governs how every agent uses git, branches, and the deploy
pipeline so that multiple agents can work at once without clobbering each other.
Companion: [`docs/app-deploy-contract.md`](docs/app-deploy-contract.md) (how apps
get deployed) and `CLAUDE.md` (machine/homelab facts).

## 0. You are the orchestrator
Own your task end-to-end. **Decompose the work and spin up sub-agents** to parallelize
(research, multi-file edits, reviews) — but **you** stay accountable for keeping it
**organized, structured, and shipped correctly through the pipelines**: branch → PR,
IaC plan→apply, Dockhand deploys, the image handshake. Sub-agents obey this same file;
don't let them bypass it, and you own the final review + integration before anything
merges or deploys. Structure and discipline are your job, not an afterthought.

**You are the *sole responsible agent* for THIS repo** — you don't act in any other
repo, and no other agent acts in yours. One repo, one owner.

## 0.1 Keep the project wiki current (every repo)
Every repo is a small markdown **wiki** — `tasks.md`, `notes.md`, `CLAUDE.md`,
`memory.md`, `index.md`, `log.md`, `chats/` at the root — that **Goblin** reads as
context and writes as memory, and that drives the Kanban board. Keeping it current IS
the job, not an afterthought. The full **OKF / LLM-wiki contract** (each file's role,
the format, the cadence) is **§10 below — read it.**

## 1. Repos & where they live
- All repos live under the **`zaieeeem` GitHub org** — never the personal `zaiemv` account.
- Clone to **`~/projects/claude-code/<reponame>`** on the host you work from.
- Each agent stays in **its own repo / domain** (see §4).

## 2. The cardinal rule: isolate your working tree
- **Never two agents in one working tree.** Each agent uses **its own clone or its own
  `git worktree`** on **its own branch**. (Sharing a checkout = silent clobbering. We hit it.)
- **Never edit another agent's repo.** Coordinate instead (§4, §5).

## 3. Git workflow (every change)
1. `git checkout main && git pull --ff-only`
2. `git checkout -b feat/<short-task>` (or `fix/`, `chore/`). **Never commit to `main` directly.**
3. Small, focused commits with clear messages. **Do NOT add AI/Anthropic attribution**
   — no `Co-Authored-By: Claude`/`<model>` trailer and no "Generated with Claude Code"
   line in commits or PR bodies.
4. `git push -u origin <branch>` → open a **PR** → the human reviews/merges.
5. **Never force-push a shared branch.** Only rebase/force-push your *own* unmerged branch.
6. Secrets **never** get committed — see §6.

## 4. Division of labor
| Agent | Owns | Edits |
|---|---|---|
| **App agents** (e.g. `code-schmode`) | app source, `Dockerfile`, image-build CI | **only their app repo** |
| **Infra agent** | `infra/`, `docker/`, ingress, secrets wiring, Dockhand deploys | **only `cafe-infrastructure`** |

App agents do **not** edit `cafe-infrastructure`; the infra agent does **not** edit app repos.

## 5. How agents coordinate (no human relay)
The **git repos are the channel** — async, in git, never by a person ferrying messages:
- The **deploy spec** for each app lives in `docs/app-deploy-contract.md`. The app agent
  fills it (image, tag, port, health, env, hostname, gating); the infra agent reads it,
  wires the deploy, and writes status back.
- Use **PRs + commit messages** for detail, **issues/PR comments** for discussion.

## 6. Secrets & IaC discipline
- **No secrets in git.** Real values come from the **secrets manager**, loaded into env
  at runtime — **Bitwarden today**, migrating to self-hosted **Infisical**
  (`docker/infisical`) as it's adopted as the runtime source. `*.tfvars` and `.env` are
  gitignored; commit only `*.example`.
- **Terraform/OpenTofu:** `tofu fmt`, plan **before** apply, isolate state per root,
  pin provider versions. Provisioning lives in `infra/` (not a top-level `terraform/`).
- **Right tool per target:** Terraform → provisioning; Dockhand → containers; Flux →
  k8s; Ansible/nix-darwin/Ventoy → host/device config. Don't force Terraform everywhere.

## 7. Server / infra deployments — always via the pipeline
This is how **anything server- or infra-side** ships. No exceptions.
- Containers deploy via **Dockhand from git** (compose under `docker/<app>/`). **Never**
  `docker run` / hand-edit compose on a host / deploy a one-off — that drifts from the repo.
- **Image handshake:** the app repo's CI builds + pushes a **versioned image** to GHCR
  (`ghcr.io/zaieeeem/<app>`, immutable `semver` + `sha-<short>` tags, package **private**).
  The infra repo **pins that tag** in `docker/<app>/compose.yaml`; Dockhand deploys + owns
  updates (scan + delay). See `docs/app-deploy-contract.md` for the full step-by-step.
- Provisioning (VMs, DNS, Access, UniFi) is **OpenTofu in `infra/`**: branch → `tofu plan`
  → review → `tofu apply`. Notify for an infra deploy by opening a PR/issue here (or
  updating the deploy spec) — see `docs/app-deploy-contract.md` §coordination.

## 8. Android app deployments
Android app repos are **client-side — they do NOT touch `cafe-infrastructure`**. Their
own pipeline (still git-by-the-book: branch → PR, secrets out of git):
1. **Build on the self-hosted runner** (`infra/gh-runner`, has the Android SDK) — CI
   builds a **signed AAB** on push/tag. The signing keystore + key passwords come from the
   secrets manager (Bitwarden→Infisical), never committed.
2. **First release is manual:** create the app in the Play Console and upload the first
   AAB by hand (Google requires the app to exist before the API will publish to it).
3. **After that, fully automated:** CI publishes every build to Google Play via the Play
   Developer API (**Gradle Play Publisher** or **fastlane `supply`**) using a Play
   **service-account JSON** (secret). Target the **internal testing track** so new builds
   land **straight on your enrolled phone**; promote to production when you choose.
4. **Privacy policy:** the **default privacy policy for all Android apps** is
   <https://gist.github.com/zaiemv/76c04f8cacddcfa8d968191c13e9392f>. Use that URL in the
   Play Console listing (and in-app where required) unless an app needs a custom one.

## 9. Tooling note
We use **git** today. `jj` (Jujutsu, git-compatible) may help with many parallel agents
later — but the discipline above (branch/worktree isolation + PRs) matters more than the
tool, and works the same under jj. Don't switch unilaterally.

## 10. The project wiki — OKF / LLM-wiki contract (Goblin)
**Goblin** is an LLM-wiki engine with a Kanban board on the front. **Every repo is a
small, typed, human-readable markdown wiki** the agent reads as context and writes as
memory. The board (cards = projects, subtask badges = `tasks.md`) is a *view* of that
wiki — nothing lives in a proprietary DB; it's all markdown in git. Keeping the wiki
current is the job.

### Format: OKF (Obsidian Knowledge Format)
OKF is **fully conformant with Google Cloud's Open Knowledge Format (OKF) v0.1** (same
acronym, our name) — so a project's wiki is portable and interoperable.
- **A directory of markdown files.** One concept = one file; the **file path is the
  concept's identity**. Subdirectories group concepts.
- **YAML frontmatter on every file.** Exactly one **required** field — `type`.
  Recommended (use what applies): `title`, `description`, `tags`, `timestamp` (ISO-8601),
  `resource` (URL to the real thing, when one exists):
  ```yaml
  ---
  type: tasks
  title: <repo> tasks
  description: One-line summary of this node
  tags: [auth, backend]
  timestamp: 2026-06-23T14:30:00Z
  resource: https://github.com/zaieeeem/<repo>
  ---
  ```
- **Reserved filenames:** `index.md` (a level's map-of-content / front door) and `log.md`
  (chronological change history) — Goblin auto-maintains both.
- **Cross-link with standard markdown links** — `see [the auth flow](./notes.md)` — so the
  wiki forms a navigable graph. (Obsidian `[[wikilinks]]` are fine for humans; write
  standard `[](path)` links for OKF portability.)
- The body after the frontmatter is freeform.

### Per-project file contract (the SAME at every repo root)
No two-tier projects — Goblin-created or existing-repo, the files are the same (linking
scaffolds any missing):

| File | `type:` | Owner | Agent behavior |
|---|---|---|---|
| `tasks.md` | `tasks` | shared | checklist → the card's subtask badge; agent keeps current, human checks items |
| `notes.md` | `note` | **human** | agent **reads for context, NEVER overwrites** — the human's scratchpad + steering channel |
| `CLAUDE.md` | `config` | human | operating instructions, auto-loaded by `claude` each turn (separate from this AGENTS.md) |
| `memory.md` | `memory` | agent | the agent's durable learned facts (agent reads + rewrites) |
| `index.md` | `index` | agent (auto) | map-of-content; refreshed after turns |
| `log.md` | `log` | agent (auto) | append-only activity history |
| `chats/` | — | Goblin | persisted chat transcripts |
| `.goblin/board.json` | — | Goblin | the board manifest — **never hand-edit** (Goblin storage, not per-repo) |

Card **columns** (Backlog · Active · Review · Done) track the **project's** stage and are
moved by the human; `tasks.md` tracks the work inside it.

### `tasks.md` — exact format (drives the board)
OKF frontmatter `type: tasks` + a plain GitHub-style checklist:
```markdown
---
type: tasks
title: <repo> tasks
---

# Tasks

## In progress
- [ ] Wire the auth flow — started 2026-06-23

## Up next
- [ ] Add the settings screen

## Done
- [x] Bootstrap the server
```
Goblin parses `- [ ]` / `- [x]` (also `*`/`+` bullets, `[X]` on read; normalizes on
write) and shows the count as the card's subtask badge (e.g. 2/5). It **only** rewrites
checklist lines — frontmatter, headings, and prose are preserved — so group with
`## In progress` / `## Up next` / `## Done` freely. The plain format is deliberate so
**Obsidian and Vikunja read the same file**.

### How agents behave (cadence + memory)
- **Update at boundaries, not on a timer** — mark a task in-progress when you start,
  check it when you finish; don't commit noise on a clock.
- **The wiki is your long-term memory.** Write durable facts to `memory.md` and task
  state to `tasks.md` as you learn them, and **before a context compaction**; re-read
  `tasks.md` + `CLAUDE.md` + `memory.md` + `notes.md` at session start.
- **Never clobber `notes.md`** — it's the human's; read it for steering, don't rewrite it.
- `index.md` / `log.md` are mostly automatic — don't fight them.
- *(Planned)* Claude Code **hooks** enforce the mechanics — `PreCompact` flushes/commits
  the wiki before compaction, `Stop` auto-commits + refreshes `index.md` + validates
  `tasks.md`, `SessionStart` reloads it.
