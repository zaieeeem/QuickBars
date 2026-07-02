---
type: tasks
title: Bing-Bong (QuickBars fork) tasks
description: TV-experience fork roadmap (remote-first UX + 10-foot design)
timestamp: 2026-07-02T00:00:00Z
resource: https://github.com/zaieeeem/QuickBars
---

# Tasks

## In progress

- [ ] Review + merge PR `feat/bingbong-rebrand` (stacked on `feat/tv-experience`: Play identity, icon/banner, GPL attribution, play-listing kit) — started 2026-07-02
- [ ] Review + merge PR `feat/tv-experience` (shared tile foundation, generic tile redesign, media volume D-pad adjust, quiet reconnect banner) — started 2026-07-02

## Up next

Play publication (see [docs/play-listing.md](docs/play-listing.md) for the full checklist):

- [ ] **Make the repo public (or publish a source mirror) — GPLv3 blocker before any Play binary**
- [ ] New private upload keystore + Play App Signing (committed `.ci/quickbars-ci.jks` is public — never for Play)
- [ ] Billing decision: wire RevenueCat/Play products for `com.zaiemv.bingbong` or strip the Plus flow
- [ ] Create the Play Console app (Bing-Bong / com.zaiemv.bingbong), manual first AAB, TV track + declarations
- [ ] Capture 2+ TV screenshots (1920x1080) for the listing

Overlay redesign phase 4 (see [the design doc](docs/design/overlay-redesign.md) — the shared
`dpadAdjust`/`rememberDebouncedAction`/`TileIconCircle` foundation is in place, so each
of these is now a contained per-card migration):

- [ ] Fan card → tile system: D-pad steps speed percentage, value fill, accent circle *(upstream-worthy)*
- [ ] Cover card → tile system: D-pad steps position, value fill *(upstream-worthy)*
- [ ] Climate card → tile system: D-pad steps target temperature, heat/cool accent split *(upstream-worthy)*
- [ ] Lock + alarm cards → shared focus frame/icon circle (visual-only, no D-pad axis needed)

Phase 5-6 + polish:

- [ ] Bar chrome: floating panel inset from screen edge + edge scrim gradient behind the bar, overscan-safe margins (mockup-1) *(upstream-worthy)*
- [ ] Connection dot in the bar header (green/amber) next to the bar name, replacing the banner for brief outages
- [ ] Skeleton tiles while the first state snapshot loads (today the bar can render with empty states) *(upstream-worthy)*
- [ ] Apply the tile focus treatment to the settings/management screens (QuickBarStyleActivity, ManageSavedEntitiesActivity) for consistency
- [ ] Contribute the phase 1-3 direct-manipulation light tile + entity-selector search upstream to Trooped/QuickBars *(flag: needs upstream discussion — it replaces their tabs+buttons flow)*
- [ ] Contribute the domain accent palette upstream *(flag: check upstream design intent first — it changes their default ON color from theme primary)*

## Done

- [x] Bing-Bong rebrand: app label + `com.zaiemv.bingbong` id, desk-bell icon/banner set (`scripts/gen_brand_assets.py`), GPL attribution (README + About), docs/play-listing.md — 2026-07-02
- [x] Overlay redesign doc + mockups (docs/design/overlay-redesign.md)
- [x] Phase 1-3: TvTile foundation (focus frame, dpadAdjust, back dispatcher), direct-manipulation light tile, entity selector search
- [x] Shared tile foundation: domain accents, TileIconCircle, rememberDebouncedAction moved to foundation — 2026-07-02
- [x] Generic EntityCard → 10-foot tile (accent icon circle, live state line, unified focus ring, unavailable dimmed + skipped by focus) — 2026-07-02
- [x] Media player tile: direct D-pad volume w/ value fill, live "Playing · 45%" state line, BACK collapses expanded panel — 2026-07-02
- [x] Quiet reconnect banner (pulsing amber dot + "Retrying…") instead of error container — 2026-07-02
