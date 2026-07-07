---
type: note
title: Bing-Bong — Google Play listing kit
description: Production store copy, artwork spec, permission declarations, and the TV publication checklist for com.zaiemv.bingbong.
timestamp: 2026-07-07T00:00:00Z
---

# Bing-Bong — Google Play listing kit

Everything needed to publish `com.zaiemv.bingbong` to Google Play for Android TV.
Production store artwork lives in [`docs/store-assets/`](store-assets/); each piece is a
designed composition rendered to an exact-size PNG from the HTML/SVG source in
[`docs/store-assets/src/`](store-assets/src/) with headless Chrome (see that folder).

> These `docs/store-assets/` files are the **production listing set** and supersede the
> auto-generated `assets/play/` artwork (which stays as the in-repo/launcher source of
> truth via `scripts/gen_brand_assets.py`). Upload the `docs/store-assets/` files to the
> Play Console.

## App identity

| Field | Value |
|---|---|
| App name (user-facing, everywhere) | **Bing-Bong** |
| Play title | **Bing-Bong for Home Assistant** (28 chars) |
| Application id | `com.zaiemv.bingbong` (permanent — distinct from upstream `dev.trooped.tvquickbars`) |
| Category | House & Home |
| Form factor | Android TV (Leanback) — manifest requires `android.software.leanback`, touchscreen not required |
| Monetization | **Free. No ads, no in-app purchases, no subscriptions** (billing removed in #7) |
| Privacy policy | See [`PRIVACY.md`](../PRIVACY.md) — publish it at a public URL and paste that URL in Play (also required by Data safety) |

## Verified Play asset requirements (Android TV)

Verified 2026-07-07 against the Google Play Console help page
"Add preview assets to showcase your app"
(<https://support.google.com/googleplay/android-developer/answer/9866151>). A TV app
listing requires the icon, feature graphic, **TV banner**, and **at least one TV
screenshot**. Exact specs and the file provided for each:

| Asset | Play requirement (verified) | File provided |
|---|---|---|
| Hi-res icon | 512×512 px, 32-bit PNG (alpha), ≤ 1024 KB | [`store-assets/icon-512.png`](store-assets/icon-512.png) — 512×512, 32-bit, ~125 KB |
| Feature graphic | 1024×500 px, JPEG or 24-bit PNG (no alpha) | [`store-assets/feature-graphic-1024x500.png`](store-assets/feature-graphic-1024x500.png) — 1024×500, 24-bit |
| TV banner (required for TV) | 1280×720 px, JPEG or 24-bit PNG (no alpha) | [`store-assets/tv-banner-1280x720.png`](store-assets/tv-banner-1280x720.png) — 1280×720, 24-bit |
| TV screenshots | JPEG or 24-bit PNG; min dimension ≥ 320 px, max ≤ 3840 px and ≤ 2× the other side; **at least 1**, up to 8 | 5 × 16:9 PNG at **1920×1080** (see below) |

Notes verified on the same page: TV screenshots display only on Android TV devices; the
feature graphic can be cropped by Play for different placements, so all critical content
in `feature-graphic` and `tv-banner` is kept in the safe centre.

## Store listing copy

**Title (≤30):**

> Bing-Bong for Home Assistant

**Short description (≤80):**

> Home Assistant quick-bars over any app on your TV. Tap a button, control home.

**Full description (≤4000):**

> Bing-Bong puts your Home Assistant controls right on top of whatever you're watching on Android TV.
>
> Press a remote button you choose and a quick bar slides in over any app — lights, switches, scenes, media players, climate, fans, covers, cameras and more. Everything is built for the couch: big tiles, D-pad navigation and warm, readable accent colours. Press Back and you're right where you left off.
>
> WHAT YOU CAN DO
> • Quick bars over any app — control your home without leaving your show
> • Direct D-pad control — hold left/right on a tile to dim a light or change media volume
> • Trigger keys — map single, double or long presses of remote buttons to open a bar or run an action
> • Real-time — talks to your Home Assistant over your local network using the WebSocket API
> • Make it yours — per-bar themes (background, opacity, ON-state colour), screen position, and 1- or 2-column layouts
> • Camera picture-in-picture — glance at the doorbell without leaving your movie
> • TV-optimised notifications from Home Assistant, with images and action buttons
>
> BUILT FOR THE 10-FOOT EXPERIENCE
> Bing-Bong is a Leanback (Android TV) app. Every screen is fully D-pad navigable with a focus-ring design tuned for reading across the room — no touchscreen required.
>
> PRIVATE BY DESIGN
> Bing-Bong connects directly to your own Home Assistant on your local network. No account to create, no cloud, no tracking, no data collection. Your access token is stored encrypted on your device.
>
> COMPLETELY FREE
> Bing-Bong is completely free — no ads, no in-app purchases, no subscriptions. It's released under the GPL-3.0 licence and is based on QuickBars by Omri Peretz.
>
> Not set up yet? Bing-Bong includes a built-in demo mode so you can explore a sample home before connecting your own.
>
> Requires a running Home Assistant instance reachable on your local network and a Long-Lived Access Token (created in your Home Assistant profile).

## Screenshots (upload order — best first)

Captured live from the app running on a 1920×1080 Android TV surface, in the app's own
demo mode (the built-in sample home — no real server or credentials involved). The phone
status bar from the capture device was cleaned off with
[`src/screenshot-statusbar-compositor.html`](store-assets/src/screenshot-statusbar-compositor.html);
no app content was altered.

1. [`tv-screenshot-1-quickbar-tiles.png`](store-assets/tv-screenshot-1-quickbar-tiles.png) — the quick-bar tiles lit in the warm brand amber (the ON-state colour picker), showing the live tile UI.
2. [`tv-screenshot-2-entity-selector.png`](store-assets/tv-screenshot-2-entity-selector.png) — picking Home Assistant entities for a quick bar, with search.
3. [`tv-screenshot-3-welcome.png`](store-assets/tv-screenshot-3-welcome.png) — "Welcome to Bing-Bong for Home Assistant" setup (Manual / Easy).
4. [`tv-screenshot-4-quickbar-editor.png`](store-assets/tv-screenshot-4-quickbar-editor.png) — the quick-bar editor: entities, theme, screen position, grid.
5. [`tv-screenshot-5-menu.png`](store-assets/tv-screenshot-5-menu.png) — the Bing-Bong side menu (QuickBars / Entities / Trigger Keys / Settings).

## Play Console permission declarations

### Accessibility Service API declaration

The `QuickBarService` accessibility service (`isAccessibilityTool="false"`) is used solely
to observe configured hardware key presses (remote-control buttons) so quick bars can be
summoned from any screen. Prominent-disclosure text is maintained in-app as
`R.string.play_store_accessibility_disclosure`:

> This app uses the Accessibility Service API to provide its core functionality. The
> Accessibility Service is required to detect and intercept specific hardware key presses
> (e.g., from your remote control) that you configure as "trigger keys." This allows the
> app to launch your quick bars or control your Home Assistant devices even when the app
> is in the background. Bing-Bong does not use the Accessibility Service to collect any
> user data, see your personal information, or read the content of your screen. It is used
> solely for the key-press trigger feature.

### Foreground service (`FOREGROUND_SERVICE_DATA_SYNC`)

> The optional "persistent background connection" setting keeps a WebSocket to the user's
> own Home Assistant server alive (with a visible persistent notification) so
> Home-Assistant-initiated triggers and TV notifications arrive while other apps are in the
> foreground. It syncs entity state data only; the user enables and disables it explicitly
> in settings, and `RECEIVE_BOOT_COMPLETED` only restores it after reboot when that setting
> is on.

### `SYSTEM_ALERT_WINDOW` (Display over other apps)

Not a Play declaration form, but the app's core mechanism: on an explicit user action (a
configured remote key press, or a notification from the user's own Home Assistant) it
draws a quick-action bar or camera picture-in-picture over the current app, dismissed by
Back. Never used for ads, phishing, or obscuring other apps' UI. Without
`SYSTEM_ALERT_WINDOW` the app has no function.

## Data safety form

- **No data collected, no data shared.** All communication is device ↔ the user's own Home
  Assistant server on the local network (`usesCleartextTraffic="true"` because HA installs
  commonly run plain HTTP on the LAN; note this in the form's security section). The HA
  access token is stored encrypted on-device (Tink / EncryptedPrefs).
- **No in-app purchases** — the `BILLING` permission and the upgrade flow were removed (#7);
  the app ships with every feature free. Nothing to declare for purchases.

## TV (Leanback) readiness — verified in the manifest

- `<category android:name="android.intent.category.LEANBACK_LAUNCHER"/>` on `SplashActivity` ✔
- `<uses-feature android:name="android.software.leanback" android:required="true"/>` ✔
- `<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>` ✔
- `android:banner="@mipmap/ic_banner"` on `<application>` ✔
- All UI is D-pad navigable (verified live: onboarding, entity selector, quick-bar editor,
  theme editor with live tile preview, trigger keys, settings, side menu).

## Publication checklist (owner)

1. [ ] **GPL source availability (BLOCKER):** make `zaieeeem/QuickBars` public (or publish a
       public mirror linked from the listing + About screen) *before* the first binary goes
       to Play. GPLv3 §6 — binaries must ship with corresponding source.
2. [ ] **Signing:** generate a NEW private upload keystore. The committed `.ci/quickbars-ci.jks`
       is public and must never sign a Play build. Enroll in Play App Signing.
3. [ ] Create the app in Play Console (name **Bing-Bong**, id `com.zaiemv.bingbong`), opt in
       to **Android TV**; first AAB upload is manual (`:app:bundleRelease`).
4. [ ] **Store listing → Main store listing:**
       - [ ] Title / Short / Full description — paste the copy above.
       - [ ] Hi-res icon → `store-assets/icon-512.png`
       - [ ] Feature graphic → `store-assets/feature-graphic-1024x500.png`
       - [ ] **TV banner → `store-assets/tv-banner-1280x720.png`** (required for TV)
       - [ ] **Android TV screenshots** → upload `tv-screenshot-1..5` in that order (min 2; we ship 5).
5. [ ] Publish the privacy policy at a public URL (from `PRIVACY.md`) and paste it into the
       listing + **Data safety** (declare: no data collected/shared; see form notes above).
6. [ ] **App content:** complete the **Accessibility API** declaration and **Foreground
       service** (`FOREGROUND_SERVICE_DATA_SYNC`) declaration using the texts above.
7. [ ] **Content rating** questionnaire (utility, no UGC, no ads, no purchases → Everyone).
8. [ ] **TV quality review:** Google manually reviews TV apps against the
       [TV app quality guidelines](https://developer.android.com/docs/quality-guidelines/tv-app-quality)
       — D-pad-only operation, banner present, touchscreen not required (all verified above).
9. [ ] Roll out to internal testing, then promote to production when ready.

## Known follow-up (not blocking artwork, flag for a code PR)

**GPL-3.0 source availability (OWNER DECISION before production publish):** the app is a
GPL-3.0 derivative of QuickBars (Omri Peretz) distributed via Play. GPL-3.0 requires the
corresponding source to be available to recipients; the fork repo is currently PRIVATE, so
the listing copy no longer links it ("open source" claim and dead link removed 2026-07-07).
Options for the owner: publish the repo (conflicts with the current nothing-goes-public
stance — explicit owner call), publish a source tarball elsewhere, or take a written
offer-of-source approach. Decide before the production push.

Some **in-app strings still read "QuickBars"** rather than "Bing-Bong" — verified live on
v1.3.3-r5: the main hub title ("QuickBars"), the entity-import header ("Welcome to
QuickBars for HA"), the side-menu "QuickBars" nav item + the "quickbars.app" support link,
and "QuickBars App ID" in Settings. The `app_name` resources are already correct
(`app_name_full` = "Bing-Bong for Home Assistant", `app_name_short` = "Bing-Bong"); these
are separate hardcoded strings. The five chosen screenshots avoid the bare "QuickBars" hub
title. Recommend a follow-up code change to rebrand these strings before the production
push, since the naming rule is Bing-Bong on every user-facing surface. ("QuickBar"/"quick
bar" as the *feature* noun is fine and matches the listing copy.)
