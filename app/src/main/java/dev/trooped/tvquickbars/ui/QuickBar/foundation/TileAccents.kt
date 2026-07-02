package dev.trooped.tvquickbars.ui.QuickBar.foundation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.trooped.tvquickbars.R

/** The one tile shape shared by overlay cards (see docs/design/overlay-redesign.md). */
val TvTileShape = RoundedCornerShape(16.dp)

/**
 * Muted, HA-familiar accent color for an entity's domain, used for the tile icon circle
 * and value fills so each domain is recognizable at a glance from the couch:
 * lights amber, media purple, climate blue, fans cyan, scenes/scripts green,
 * locks/alarm red, covers teal, everything else a neutral blue-grey.
 */
fun domainAccentColor(entityId: String): Color = when (entityId.substringBefore('.')) {
    "light", "switch", "input_boolean" -> Color(0xFFFFB74D)          // amber
    "media_player", "remote" -> Color(0xFFBA68C8)                    // purple
    "climate", "water_heater", "humidifier" -> Color(0xFF4FC3F7)     // blue
    "fan" -> Color(0xFF4DD0E1)                                       // cyan
    "scene", "script", "button", "input_button", "automation" -> Color(0xFF81C784) // green
    "lock", "alarm_control_panel" -> Color(0xFFE57373)               // red
    "cover", "valve" -> Color(0xFF4DB6AC)                            // teal
    "camera", "custom_camera" -> Color(0xFF7986CB)                   // indigo
    else -> Color(0xFF90A4AE)                                        // blue-grey
}

/** Simple luma heuristic (sRGB) to pick black/white content on an accent background. */
fun accentContentFor(bg: Color): Color {
    val luma = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return if (luma > 0.6f) Color.Black else Color.White
}

/**
 * Resolves the accent color a tile should use for its active state.
 *
 * Explicit user choices from the bar's style settings (custom RGB, amber, tertiary, error)
 * always win; the default ("colorPrimary") maps to the entity's [domainAccentColor] so an
 * untouched install gets the domain-tinted look.
 */
@Composable
fun resolveTileAccent(
    entityId: String,
    onStateColor: String,
    customOnStateColor: List<Int>?
): Color = when {
    onStateColor.equals("custom", ignoreCase = true) &&
            customOnStateColor != null && customOnStateColor.size >= 3 -> Color(
        android.graphics.Color.rgb(
            customOnStateColor[0].coerceIn(0, 255),
            customOnStateColor[1].coerceIn(0, 255),
            customOnStateColor[2].coerceIn(0, 255)
        )
    )
    onStateColor == "colorAmber500" -> colorResource(id = R.color.md_theme_Amber500)
    onStateColor == "colorTertiary" -> colorResource(id = R.color.md_theme_tertiary)
    onStateColor == "colorError" -> colorResource(id = R.color.md_theme_error)
    else -> domainAccentColor(entityId)
}

/**
 * The shared tile icon treatment: a circle that is solid accent while the entity is
 * active and a faint content-tinted disc while it is off, with the entity icon inside.
 */
@Composable
fun TileIconCircle(
    iconRes: Int,
    name: String,
    active: Boolean,
    accentColor: Color,
    accentContentColor: Color,
    contentColor: Color,
    size: Dp = 36.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) accentColor else contentColor.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = SafePainterResource(id = iconRes),
            contentDescription = name,
            modifier = Modifier.size(size * 0.55f),
            colorFilter = ColorFilter.tint(if (active) accentContentColor else contentColor)
        )
    }
}
