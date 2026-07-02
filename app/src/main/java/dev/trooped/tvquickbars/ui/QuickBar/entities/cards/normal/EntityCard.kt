package dev.trooped.tvquickbars.ui.QuickBar.entities.cards.normal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.trooped.tvquickbars.R
import dev.trooped.tvquickbars.data.EntityAction
import dev.trooped.tvquickbars.data.EntityItem
import dev.trooped.tvquickbars.data.PressType
import dev.trooped.tvquickbars.ha.HomeAssistantClient
import dev.trooped.tvquickbars.persistence.SavedEntitiesManager
import dev.trooped.tvquickbars.ui.EntityIconMapper
import dev.trooped.tvquickbars.ui.QuickBar.foundation.TileIconCircle
import dev.trooped.tvquickbars.ui.QuickBar.foundation.TvTileShape
import dev.trooped.tvquickbars.ui.QuickBar.foundation.accentContentFor
import dev.trooped.tvquickbars.ui.QuickBar.foundation.formatBinarySensorState
import dev.trooped.tvquickbars.ui.QuickBar.foundation.formatTimestamp
import dev.trooped.tvquickbars.ui.QuickBar.foundation.resolveTileAccent
import dev.trooped.tvquickbars.ui.QuickBar.foundation.tvFocusFrame
import dev.trooped.tvquickbars.utils.EntityActionExecutor
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.text.ifEmpty

/**
 * EntityCard
 * The shared tile for "normal" entities (switches, scenes, scripts, sensors, buttons,
 * cameras, simple lights, ...). Follows the overlay tile system: constant dark surface,
 * a domain-accented icon circle that goes solid while the entity is active, a name plus
 * a live state line, and the shared white-ring focus treatment. Unavailable entities are
 * dimmed and skipped by D-pad focus instead of being styled-but-dead.
 *
 * @param entity The entity to display.
 * @param haClient The HomeAssistantClient to use.
 * @param onStateColor The user-selected on-state color key ("colorPrimary" = domain accent).
 * @param modifier The modifier to apply to the card.
 * @param isHorizontal Whether the card should be displayed horizontally.
 */
@Composable
fun EntityCard(
    entity: EntityItem,
    haClient: HomeAssistantClient?,
    onStateColor: String,
    customOnStateColor: List<Int>?,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false,
    isEntityInitialized: Boolean = false
) {
    val name = remember(entity.customName, entity.friendlyName) {
        entity.customName.ifEmpty { entity.friendlyName }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val context = LocalContext.current
    val savedEntitiesManager = remember {
        SavedEntitiesManager(context)
    }

    var buttonPressed by remember { mutableStateOf(false) }

    // Identify if these are special/different entities
    val isSensor = entity.id.startsWith("sensor.")
    val isBinarySensor = entity.id.startsWith("binary_sensor.")
    val isScene = entity.id.startsWith("scene.")
    val isButton = entity.id.startsWith("button.") || entity.id.startsWith("input_button.")
    val isScript = entity.id.startsWith("script.")
    val isCamera = entity.id.startsWith("camera.") || entity.id.startsWith("custom_camera.")

    val isDisabledState = entity.state in listOf("unavailable", "unknown")

    val isClickable = when {
        isSensor || isBinarySensor -> false // Rule 1: Sensors are never clickable.
        isDisabledState -> isButton || isScript || isScene // Rule 2: If disabled, only buttons/scripts are clickable.
        else -> true // Rule 3: All other enabled entities are clickable.
    }

    // Unavailable entities should not be D-pad focus stops (unless still actionable).
    val isFocusable = isClickable || ((isSensor || isBinarySensor) && !isDisabledState)

    // camera-specific "is on" rule
    val isOnVisual = remember(entity.state, isDisabledState) {
        val cameraIsOn = isCamera && !isDisabledState && !entity.state.equals("off", ignoreCase = true)
        val genericEntityIsOn = !isCamera && entity.state == "on"
        cameraIsOn || genericEntityIsOn
    }

    val accentColor = resolveTileAccent(entity.id, onStateColor, customOnStateColor)
    val accentContentColor = accentContentFor(accentColor)

    val surfaceColor = colorResource(id = R.color.md_theme_surfaceVariant)
    val onSurfaceColor = colorResource(id = R.color.md_theme_onSurface)
    // A soft accent wash over the surface makes the ON state readable from the couch
    // without flipping the whole tile to a saturated color.
    val backgroundColor = if (isOnVisual) {
        accentColor.copy(alpha = 0.22f).compositeOver(surfaceColor)
    } else {
        surfaceColor
    }
    val contentColor = if (isDisabledState) onSurfaceColor.copy(alpha = 0.5f) else onSurfaceColor

    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(200),
        label = "bgColorAnim"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = contentColor,
        animationSpec = tween(200),
        label = "contentColorAnim"
    )

    val iconRes =
        remember(entity.id, entity.state, entity.customIconOnName, entity.customIconOffName) {
            EntityIconMapper.getFinalIconForEntity(entity) ?: R.drawable.ic_default
        }

    var isPressed by remember { mutableStateOf(false) }

    // This effect listens for physical press and release events
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release -> isPressed = false
                is PressInteraction.Cancel -> isPressed = false
            }
        }
    }

    // This animation smoothly scales the icon based on the isPressed state
    val iconScaleLongPress by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconPressAnim"
    )

    val iconScaleButtonPress by animateFloatAsState(
        targetValue = if (buttonPressed) 1.3f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        // When the "pop" animation finishes, this listener resets the state,
        // which causes the icon to animate back to its normal size.
        finishedListener = { buttonPressed = false },
        label = "iconScaleAnim"
    )

    val combinedIconScale = maxOf(iconScaleLongPress, iconScaleButtonPress)

    val handlePress: (PressType) -> Unit =
        remember(entity.id, haClient, savedEntitiesManager, isEntityInitialized) {
            { press ->
                // First, determine if the action for this specific press should have the pop animation.
                val isButtonEntity =
                    entity.id.startsWith("button.") || entity.id.startsWith("input_button.")
                val action = entity.pressActions[press] ?: EntityAction.Default
                val isMomentary = isButtonEntity || isScene || isScript

                val shouldAnimate = isMomentary && (
                        // It's the default single press on a momentary entity
                        (press == PressType.SINGLE && action is EntityAction.Default) ||
                                // Or it's a specific service call to "press"
                                (action is EntityAction.ServiceCall && action.service == "press")
                        )

                if (shouldAnimate) {
                    buttonPressed = true
                }

                if (isEntityInitialized) {
                    EntityActionExecutor.perform(
                        entity = entity,
                        press = press,
                        haClient = haClient,
                        savedEntitiesManager = savedEntitiesManager,
                        onExpand = {}
                    )
                }
            }
        }

    Card(
        modifier = modifier
            .tvFocusFrame(isFocused, TvTileShape)
            .combinedClickable(
                enabled = isClickable,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = animatedContentColor),

                onClick = { handlePress(PressType.SINGLE) },
                onLongClick = {
                    /* keep long-press ripple / haptic if you wish */
                    handlePress(PressType.LONG)
                }
            )
            .focusable(enabled = isFocusable, interactionSource = interactionSource)
            .alpha(if (isDisabledState) 0.55f else 1f),
        shape = TvTileShape,
        colors = CardDefaults.cardColors(
            containerColor = animatedBackgroundColor,
            contentColor = animatedContentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {

        val stateText = when {
            isSensor -> {
                val attributes = entity.attributes
                val deviceClass = attributes?.optString("device_class", "") ?: ""

                if (deviceClass == "timestamp") {
                    formatTimestamp(entity.state)
                } else {
                    val unit = attributes?.optString("unit_of_measurement", "") ?: ""
                    val formattedState = formatNumericSmart(entity.state, unit, attributes)
                    if (unit.isBlank()) formattedState else "$formattedState $unit"
                }
            }

            isBinarySensor -> formatBinarySensorState(entity)
            isDisabledState -> entity.state.replaceFirstChar { it.uppercase() }
            // Scene/button states are "last triggered" timestamps — not useful at a glance.
            isScene || isButton -> ""
            isScript -> if (entity.state == "on") "Running" else ""
            entity.state.equals("on", ignoreCase = true) -> "On"
            entity.state.equals("off", ignoreCase = true) -> "Off"
            else -> entity.state.replaceFirstChar { it.uppercase() }
        }

        val iconActive = isOnVisual && !isSensor && !isBinarySensor

        if (isHorizontal) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.scale(combinedIconScale)) {
                    TileIconCircle(
                        iconRes = iconRes,
                        name = name,
                        active = iconActive,
                        accentColor = accentColor,
                        accentContentColor = accentContentColor,
                        contentColor = animatedContentColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = name,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (stateText.isNotEmpty()) {
                    // Force LTR layout direction for state text
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = stateText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = animatedContentColor.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.scale(combinedIconScale)) {
                    TileIconCircle(
                        iconRes = iconRes,
                        name = name,
                        active = iconActive,
                        accentColor = accentColor,
                        accentContentColor = accentContentColor,
                        contentColor = animatedContentColor
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (stateText.isNotEmpty()) {
                        // Force LTR layout direction for state text
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = stateText,
                                fontSize = 12.sp,
                                color = animatedContentColor.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats a numeric state string into a human-readable format based on precision rules.
 *
 * The formatting logic follows this priority:
 * 1. Returns "unknown" or "unavailable" as-is.
 * 2. If the value is a whole number, returns it without decimals.
 * 3. Uses `display_precision` from [attrs] if available (0-6).
 * 4. Otherwise, matches the source precision (number of digits after the decimal point) up to 3 places.
 * 5. As a fallback, determines decimal count based on the magnitude of the value.
 *
 */
private fun formatNumericSmart(rawState: String, unit: String, attrs: JSONObject?): String {
    val raw = rawState.trim()

    // pass through non-numeric states
    if (raw.equals("unknown", true) || raw.equals("unavailable", true)) return raw

    val bd = raw.toBigDecimalOrNull() ?: return raw

    // Whole numbers -> no decimals
    if (bd.stripTrailingZeros().scale() <= 0) {
        return bd.toPlainString()
    }

    // 1) allow HA to hint precision
    val haPrec = attrs?.opt("display_precision")?.toString()?.toIntOrNull()
    val decimals = when {
        haPrec != null && haPrec in 0..6 -> haPrec

        // 2) respect source precision up to 3 decimals
        raw.contains('.') -> minOf(raw.substringAfter('.', "").length, 3)

        // 3) fallback by magnitude when string has no '.'
        else -> {
            val abs = bd.abs()
            when {
                abs >= BigDecimal("100") -> 0
                abs >= BigDecimal("10")  -> 1
                abs >= BigDecimal("1")   -> 2
                else                     -> 3
            }
        }
    }

    val scaled = bd.setScale(decimals, RoundingMode.HALF_UP)
    return scaled.stripTrailingZeros().toPlainString()
}
