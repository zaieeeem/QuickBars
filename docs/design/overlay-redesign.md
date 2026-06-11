# QuickBar Overlay Redesign

Goal: kill the "click the card, then click +/− repeatedly, then find the close button"
flow, and make the bar look like a modern TV surface instead of a stack of flat cards.

## Core idea: every tile IS the slider

The D-pad axis perpendicular to the list scroll direction is unused today.
In a vertical (left/right edge) bar, LEFT/RIGHT do nothing on a focused card.
That axis becomes direct value adjustment:

| Input | Action |
|---|---|
| OK | Primary action: toggle light/switch, run scene/script, play-pause media, lock/unlock |
| ◀ ▶ (tap) | Step the primary value ±5 (brightness %, volume %, setpoint, cover position) |
| ◀ ▶ (hold) | Continuous ramp with key-repeat |
| ▲ ▼ | Move between tiles (unchanged) |
| OK (hold) | Expand tile in place for secondary controls |
| BACK | Collapse expanded tile → close bar (no on-screen close button needed) |

Horizontal (top/bottom) bars swap the axes.

Dimming a light goes from *click card → expand → navigate to "−" → click N times →
navigate to close → click* down to *focus the tile, hold ▶*. One focus stop instead of four.

### Why this is safe on TV remotes
`onPreviewKeyEvent` on the focused tile intercepts `KeyEvent.DPAD_LEFT/RIGHT` only when
the tile has an adjustable value; otherwise the event falls through (so non-adjustable
tiles don't trap focus). Value sends reuse the existing `rememberDebouncedAction`
(Light.kt:148) so HA isn't flooded during a ramp.

## Expanded tile (long-press) replaces the tabs+buttons panel

Today's Light card expands into a tab strip (Brightness/Temperature/Color), each tab
showing `[−] [value] [+]` buttons (Light.kt:861-871). Replacement: the tile grows in
place and stacks up to three control **rows**:

- **Brightness** — slider track, ◀▶ drags it
- **Warmth** — kelvin gradient track, ◀▶ drags it
- **Color** — swatch row, ◀▶ moves the selection ring, OK applies

▲▼ moves between rows, BACK collapses. No tabs, no plus/minus buttons, no nested
focus traps. Climate (mode row + setpoint row), fan (speed row), media (volume row +
source row) follow the same pattern, so the per-domain cards collapse into one shared
`ExpandableTile` scaffold with domain-specific rows.

## Visual system (see mockup-3)

- **Tile**: 16dp radius, surface `#23262E` @ 94%, 44dp icon circle, 16sp name +
  13sp state. The tile background carries a **value fill** — an accent-tinted region
  whose width = current brightness/volume/position. State is visible at a glance
  without focusing anything (today brightness is invisible until you expand).
- **Domain accents** (HA-familiar): lights `#FFB74D`, climate heat `#FF7043` /
  cool `#4FC3F7`, media `#BA68C8`, locks/covers `#81C784`, alarm `#E57373`,
  scenes/scripts `#90A4AE`. Icon circle is solid accent when active, white@8% when off.
- **One focus treatment everywhere**: 2.5dp white ring + 1.03 scale. Replaces the
  current mix of white border (EntityCard.kt:281), shadow elevation
  (ComposeMainActivity.kt:576), and background swaps. Adjustable tiles additionally
  show ◀ ▶ chevrons while focused.
- **Bar chrome**: floating rounded panel (24dp) inset from the screen edge, with an
  edge scrim gradient behind it for legibility over bright content. Header = bar name
  + connection dot. Footer = remote hint line. The close icon button is removed;
  BACK closes (reclaims a focus stop and matches every native TV surface).
- **Unavailable entities**: 50% opacity and skipped by D-pad focus instead of styled-
  but-dead.

## Implementation sketch (rough order)

1. `ExpandableTile` scaffold + value-fill rendering + focus ring modifier (shared).
2. Key handling: `onPreviewKeyEvent` adjust + long-press expand + BACK collapse.
3. Migrate Light → scaffold (delete tab strip + AnimatedIconButton +/− usage).
4. Migrate climate / fan / cover / media / lock / alarm.
5. Bar chrome (panel, scrim, header/footer, remove CloseButton).
6. Apply the same focus ring + accents to the settings screens for consistency.

Steps 1–3 are shippable on their own; old cards keep working until migrated.
