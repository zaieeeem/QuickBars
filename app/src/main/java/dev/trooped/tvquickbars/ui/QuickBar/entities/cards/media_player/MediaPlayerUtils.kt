package dev.trooped.tvquickbars.ui.QuickBar.entities.cards.media_player

import dev.trooped.tvquickbars.data.EntityItem

/**
 * Media Player Utilities
 *
 */
fun isMediaPlaying(state: String): Boolean = state.equals("playing", ignoreCase = true)
fun isMediaOn(state: String): Boolean = state.lowercase() != "off"

fun isMediaMuted(entity: EntityItem): Boolean {
    return entity.attributes?.optBoolean("is_volume_muted", false) ?: false
}

/** Current volume as 0-100, or null when the player does not report `volume_level`. */
fun mediaVolumePercent(entity: EntityItem): Int? {
    val v = entity.attributes?.optDouble("volume_level", Double.NaN) ?: Double.NaN
    return if (v.isNaN()) null else (v.coerceIn(0.0, 1.0) * 100.0 + 0.5).toInt()
}

/** MediaPlayerEntityFeature.VOLUME_SET bit from HA's `supported_features`. */
fun supportsVolumeSet(entity: EntityItem): Boolean {
    val features = entity.attributes?.optInt("supported_features", 0) ?: 0
    return (features and 4) != 0
}