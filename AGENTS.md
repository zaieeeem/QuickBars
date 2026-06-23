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

## 0.1 Keep `tasks.md` current (every repo)
Every repo has a **`tasks.md`** at its root — the live to-do for *this* repo. The
responsible agent **keeps it up to date**: what's pending, in progress, and done, in
short scannable lines, updated as work changes (not just at the end). It's read
**automatically by a homelab-wide todo integration**, so treat it as a machine-read
source of truth — keep it accurate and current.

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
