---
type: memory
title: QuickBars memory
description: Durable facts the agent learned about this project.
timestamp: 2026-07-02T00:00:00Z
---

# Memory

## What the app is
- Android TV overlay app (`dev.trooped.tvquickbars`, "QuickBars for Home Assistant"):
  remote-key-triggered overlay bars of HA entities, drawn over whatever is on screen by
  `services/QuickBarService`. Fork of Trooped/QuickBars (fork default branch is
  `claude/fervent-bohr-omsss5` = upstream main + PR #1 + OKF wiki files; `main` lags it).

## Stack / build
- Kotlin + Jetpack Compose (Material3) for the overlay + most screens; some legacy
  Leanback/XML activities. Overlay UI lives under `app/src/main/java/.../ui/QuickBar/`.
- Build: gradle wrapper 8.13, AGP 8.13.1, Kotlin 2.2.20, compileSdk 36, minSdk 28.
  Needs `JAVA_HOME=/opt/homebrew/opt/openjdk@17` and `local.properties` with
  `sdk.dir=$HOME/Library/Android/sdk`. `./gradlew :app:assembleDebug` works;
  CI (`.github/workflows`) builds a **signed assembleRelease** (R8 on) — check
  `:app:minifyReleaseWithR8` locally before pushing UI changes.
- Watch out: piping gradle output through `grep -iE "error"` can miss failures; check
  BUILD SUCCESSFUL/FAILED lines instead.

## Overlay tile system (docs/design/overlay-redesign.md)
- Foundation in `ui/QuickBar/foundation/`: `TvTile.kt` (tvFocusFrame, dpadAdjust,
  LocalBarAdjustAxis, OverlayBackDispatcher), `TileAccents.kt` (TvTileShape, domain
  accent palette, resolveTileAccent, TileIconCircle), `UtilityFunctions.kt`
  (rememberDebouncedAction, state formatters).
- Pattern for a "direct-manipulation" card: constant surfaceVariant background, accent
  value fill (alpha 0.35) sized by the live value, TileIconCircle active=on, D-pad axis
  perpendicular to scrolling adjusts the value (see Light.kt and MediaPlayer.kt),
  BACK collapse via OverlayBackDispatcher.
- User style setting `onStateColor` default is `"colorPrimary"` → mapped to domain
  accent; explicit choices (custom RGB/amber/tertiary/error) still win.
- Fan/cover/climate/lock/alarm cards are NOT yet migrated (roadmap in tasks.md).

## Rules that bit before
- AGENTS.md forbids AI attribution in commits/PR bodies (overrides default trailers).
- notes.md is human-owned — never overwrite.
- The app already has an onboarding activity (`ui/OnboardingActivity.kt`) — don't add
  a second first-run flow without checking it.
