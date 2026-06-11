package dev.trooped.tvquickbars.ui.QuickBar.foundation

import androidx.compose.foundation.border
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

/**
 * Which D-pad axis a focused tile may consume for direct value adjustment.
 * Depends on the bar layout: vertical lists leave left/right free, horizontal bars leave
 * up/down free, and the 2-column grid needs all four directions for navigation.
 */
enum class BarAdjustAxis { HORIZONTAL, VERTICAL, NONE }

val LocalBarAdjustAxis = compositionLocalOf { BarAdjustAxis.HORIZONTAL }

/**
 * The single focus treatment shared by overlay tiles: a white ring plus a slight scale-up.
 * Use this instead of per-card borders/shadows so D-pad focus looks the same everywhere.
 */
fun Modifier.tvFocusFrame(isFocused: Boolean, shape: Shape): Modifier = this
    .scale(if (isFocused) 1.03f else 1f)
    .border(
        width = if (isFocused) 2.5.dp else 0.dp,
        color = if (isFocused) Color.White else Color.Transparent,
        shape = shape
    )

/**
 * Direct D-pad value adjustment for a focused tile or control row.
 *
 * Consumes the D-pad axis that is perpendicular to list scrolling and reports steps via
 * [onAdjust] (-1 / +1). Key repeats arrive as repeated down events, so holding the key
 * ramps the value continuously. The matching up events are consumed too so they cannot
 * trigger focus moves or clicks.
 *
 * @param adjustVertically false = left/right adjusts (vertical bars), true = up/down
 *   adjusts (horizontal bars, where left/right must keep moving between tiles).
 */
fun Modifier.dpadAdjust(
    enabled: Boolean,
    adjustVertically: Boolean = false,
    onAdjust: (Int) -> Unit
): Modifier {
    if (!enabled) return this
    return this.onPreviewKeyEvent { event ->
        val direction = if (adjustVertically) {
            when (event.key) {
                Key.DirectionUp -> +1
                Key.DirectionDown -> -1
                else -> return@onPreviewKeyEvent false
            }
        } else {
            when (event.key) {
                Key.DirectionLeft -> -1
                Key.DirectionRight -> +1
                else -> return@onPreviewKeyEvent false
            }
        }
        if (event.type == KeyEventType.KeyDown) onAdjust(direction)
        true
    }
}

/**
 * Lets overlay content claim a BACK press before the QuickBarService closes the bar.
 *
 * An expanded tile registers a handler while it is expanded; the service calls [dispatch]
 * when BACK is released and only hides the overlay if no handler consumed the press.
 * Handlers are dispatched most-recently-registered first.
 */
object OverlayBackDispatcher {
    private val handlers = mutableListOf<() -> Boolean>()

    fun register(handler: () -> Boolean) {
        handlers.add(handler)
    }

    fun unregister(handler: () -> Boolean) {
        handlers.remove(handler)
    }

    fun dispatch(): Boolean {
        for (handler in handlers.asReversed()) {
            if (handler()) return true
        }
        return false
    }
}
