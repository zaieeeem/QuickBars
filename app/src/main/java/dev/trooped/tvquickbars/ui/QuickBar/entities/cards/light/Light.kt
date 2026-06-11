package dev.trooped.tvquickbars.ui.QuickBar.entities.cards.light

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.trooped.tvquickbars.R
import dev.trooped.tvquickbars.data.EntityItem
import dev.trooped.tvquickbars.data.PressType
import dev.trooped.tvquickbars.data.computeLightCaps
import dev.trooped.tvquickbars.ha.HomeAssistantClient
import dev.trooped.tvquickbars.persistence.SavedEntitiesManager
import dev.trooped.tvquickbars.ui.EntityIconMapper
import dev.trooped.tvquickbars.ui.QuickBar.controls.PowerButton
import dev.trooped.tvquickbars.ui.QuickBar.entities.cards.normal.EntityCard
import dev.trooped.tvquickbars.ui.QuickBar.foundation.BarAdjustAxis
import dev.trooped.tvquickbars.ui.QuickBar.foundation.LocalBarAdjustAxis
import dev.trooped.tvquickbars.ui.QuickBar.foundation.OverlayBackDispatcher
import dev.trooped.tvquickbars.ui.QuickBar.foundation.SafePainterResource
import dev.trooped.tvquickbars.ui.QuickBar.foundation.dpadAdjust
import dev.trooped.tvquickbars.ui.QuickBar.foundation.getTypeSafe
import dev.trooped.tvquickbars.ui.QuickBar.foundation.tvFocusFrame
import dev.trooped.tvquickbars.utils.EntityActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.text.ifEmpty

/**
 * UI state representation for a light entity, containing all the necessary information
 * to render brightness, color temperature, and RGB controls.
 *
 * @property name The display name of the light (custom name or friendly name).
 * @property isOn Whether the light is currently turned on.
 * @property brightnessPercent The current brightness level scaled from 0 to 100.
 * @property currentKelvin The current color temperature in Kelvin.
 * @property minKelvin The minimum supported color temperature in Kelvin.
 * @property maxKelvin The maximum supported color temperature in Kelvin.
 * @property rgbColor The current RGB color of the light.
 * @property indicatorColor A derived color used for UI indicators, representing either the RGB color or color temperature.
 * @property tabLabels A list of available control categories (e.g., "Brightness", "Temperature", "Color") based on entity capabilities.
 * @property supportsColorTemp Indicates if the light entity supports color temperature adjustments.
 * @property supportsRgbColor Indicates if the light entity supports RGB color adjustments.
 */
data class LightUiState(
    val name: String,
    val isOn: Boolean,
    val brightnessPercent: Int,
    val currentKelvin: Int,
    val minKelvin: Int,
    val maxKelvin: Int,
    val rgbColor: Color,
    val indicatorColor: Color?,
    val tabLabels: List<String>,
    val supportsColorTemp: Boolean,
    val supportsRgbColor: Boolean
)

/**
 * A helper function that debounces updates to a state value, primarily used for UI controls
 * (like sliders or color pickers) that trigger frequent network calls.
 *
 * It maintains a local state that updates immediately for a responsive UI, while delaying
 * the execution of the [onSend] callback until the user has stopped interacting for [delayMs].
 *
 * @param T The type of value being managed.
 * @param initialValue The starting value, typically synchronized with the external entity state.
 * @param delayMs The debounce timeout in milliseconds. Defaults to 180ms.
 * @param onSend The callback to execute after the debounce delay (e.g., calling a Home Assistant service).
 * @return A [Pair] containing the current local value and a function to update that value.
 */
@Composable
fun <T> rememberDebouncedAction(
    initialValue: T,
    delayMs: Long = 180L,
    onSend: (T) -> Unit
): Pair<T, (T) -> Unit> {
    val scope = rememberCoroutineScope()
    var isChanging by remember { mutableStateOf(false) }
    var sendJob by remember { mutableStateOf<Job?>(null) }
    var localValue by remember(initialValue) { mutableStateOf(initialValue) }

    LaunchedEffect(initialValue) {
        if (!isChanging) {
            localValue = initialValue
        }
    }

    val updateValue: (T) -> Unit = { newValue ->
        isChanging = true
        localValue = newValue
        sendJob?.cancel()
        sendJob = scope.launch {
            delay(delayMs)
            isChanging = false
            onSend(newValue)
        }
    }

    return localValue to updateValue
}

/**
 * Remembers and computes the [LightUiState] for a given light entity.
 *
 * This function handles the logic for extracting and calculating light-specific properties from
 * the entity's attributes, such as brightness percentage, color temperature (Kelvin),
 * and RGB color values. It also determines which control rows (Brightness, Temperature, Color)
 * should be visible based on the entity's capabilities and user preferences.
 *
 * @param entity The [EntityItem] representing the light.
 * @param isOn Boolean indicating if the light is currently powered on.
 * @param supportsColorTemp Boolean indicating if the light hardware supports color temperature adjustments.
 * @param supportsRgbColor Boolean indicating if the light hardware supports RGB color selection.
 * @return A [LightUiState] containing the processed UI properties for the light card.
 */
@Composable
fun rememberLightUiState(entity: EntityItem, isOn: Boolean, supportsColorTemp: Boolean, supportsRgbColor: Boolean): LightUiState {
    val attributes = entity.attributes ?: JSONObject()
    val name = remember(entity.customName, entity.friendlyName) {
        entity.customName.ifEmpty { entity.friendlyName }
    }

    val brightnessPercent = remember(entity.attributes) {
        fun readNumberLike(key: String): Double? {
            val v = attributes.opt(key)
            return when (v) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull()
                else -> null
            }
        }
        val raw = readNumberLike("brightness")
            ?: readNumberLike("brightness_pct")?.let { it.coerceIn(0.0, 100.0) * 2.55 }
            ?: readNumberLike("level")
            ?: 0.0
        val clamped = raw.coerceIn(0.0, 255.0)
        ((clamped * 100.0 + 127.0) / 255.0).toInt().coerceIn(0, 100)
    }

    val showBrightnessControls = entity.lastKnownState.getTypeSafe("show_brightness_controls", true)
    val showWarmthControls = entity.lastKnownState.getTypeSafe("show_warmth_controls", true)
    val showColorControls = entity.lastKnownState.getTypeSafe("show_color_controls", true)

    val (minKelvin, maxKelvin) = remember(attributes) { getEffectiveKelvinRange(attributes) }
    val currentKelvin = remember(attributes) { getCurrentKelvin(attributes) }

    val rgbColor = remember(attributes) {
        try {
            val rgb = attributes.optJSONArray("rgb_color")
            if (rgb != null && rgb.length() == 3) {
                Color(rgb.getInt(0), rgb.getInt(1), rgb.getInt(2))
            } else Color.White
        } catch (e: Exception) {
            Color.White
        }
    }

    val indicatorColor: Color? = remember(isOn, supportsRgbColor, supportsColorTemp, rgbColor, currentKelvin, minKelvin, maxKelvin) {
        if (!isOn) null
        else if (supportsRgbColor) rgbColor
        else if (supportsColorTemp && currentKelvin in minKelvin..maxKelvin) colorFromKelvin(
            currentKelvin,
            minKelvin,
            maxKelvin
        )
        else null
    }

    val tabLabels = remember(showBrightnessControls, showWarmthControls, showColorControls, supportsColorTemp, supportsRgbColor) {
        val labels = mutableListOf<String>()
        if (showBrightnessControls) labels.add("Brightness")
        if (showWarmthControls && supportsColorTemp) labels.add("Temperature")
        if (showColorControls && supportsRgbColor) labels.add("Color")
        labels
    }

    return LightUiState(
        name = name,
        isOn = isOn,
        brightnessPercent = brightnessPercent,
        currentKelvin = currentKelvin,
        minKelvin = minKelvin,
        maxKelvin = maxKelvin,
        rgbColor = rgbColor,
        indicatorColor = indicatorColor,
        tabLabels = tabLabels,
        supportsColorTemp = supportsColorTemp,
        supportsRgbColor = supportsRgbColor
    )
}

/**
 * A specialized card component for Home Assistant light entities.
 *
 * Simple on/off lights delegate to a standard [EntityCard]. Dimmable lights render as a
 * direct-manipulation tile:
 * - OK toggles the light, D-pad left/right (up/down in horizontal bars) adjusts brightness
 *   directly while the tile is focused — the tile background fills to show the level.
 * - Long-press expands the tile in place with rows for brightness, warmth, and color;
 *   up/down moves between rows, left/right adjusts, BACK collapses.
 */
@Composable
fun LightEntityCard(
    entity: EntityItem,
    haClient: HomeAssistantClient?,
    onStateColor: String,
    customOnStateColor: List<Int>? = null,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false,
    isEntityInitialized: Boolean
) {
    val context = LocalContext.current

    LaunchedEffect(entity.id) {
        if (entity.lastKnownState == null) entity.lastKnownState = mutableMapOf()
        val savedEntitiesManager = SavedEntitiesManager(context.applicationContext)
        savedEntitiesManager.applyDefaultLightOptions(entity)
    }

    val caps = remember(entity.id, entity.attributes, entity.lastKnownState) {
        computeLightCaps(entity.attributes, entity.lastKnownState)
    }

    if (caps.isSimple) {
        EntityCard(
            entity = entity,
            haClient = haClient,
            onStateColor = onStateColor,
            customOnStateColor = customOnStateColor,
            modifier = modifier,
            isHorizontal = isHorizontal,
            isEntityInitialized = isEntityInitialized
        )
    } else {
        LightTile(
            entity = entity,
            haClient = haClient,
            onStateColor = onStateColor,
            customOnStateColor = customOnStateColor,
            modifier = modifier,
            isHorizontal = isHorizontal,
            isEntityInitialized = isEntityInitialized,
            supportsBrightness = caps.brightness,
            supportsColorTemp = caps.colorTemp,
            supportsRgbColor = caps.color
        )
    }
}

private const val BrightnessStepPercent = 5
private val TileShape = RoundedCornerShape(16.dp)
private val ControlRowShape = RoundedCornerShape(12.dp)

@Composable
private fun LightTile(
    entity: EntityItem,
    haClient: HomeAssistantClient?,
    onStateColor: String,
    customOnStateColor: List<Int>?,
    modifier: Modifier,
    isHorizontal: Boolean,
    isEntityInitialized: Boolean,
    supportsBrightness: Boolean,
    supportsColorTemp: Boolean,
    supportsRgbColor: Boolean
) {
    val context = LocalContext.current
    val adjustAxis = LocalBarAdjustAxis.current
    var expanded by remember { mutableStateOf(false) }
    val cardFocusRequester = remember { FocusRequester() }
    val firstRowFocusRequester = remember { FocusRequester() }
    val wasCardFocused = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) wasCardFocused.value = true
    }

    val isOn = entity.state == "on"
    val isEnabled = entity.state !in listOf("unavailable", "unknown")

    val uiState = rememberLightUiState(
        entity = entity,
        isOn = isOn,
        supportsColorTemp = supportsColorTemp,
        supportsRgbColor = supportsRgbColor
    )

    val accentColor = when {
        onStateColor.equals("custom", ignoreCase = true) && customOnStateColor != null && customOnStateColor.size >= 3 -> {
            Color(android.graphics.Color.rgb(customOnStateColor[0].coerceIn(0, 255), customOnStateColor[1].coerceIn(0, 255), customOnStateColor[2].coerceIn(0, 255)))
        }
        onStateColor == "colorAmber500" -> colorResource(id = R.color.md_theme_Amber500)
        onStateColor == "colorTertiary" -> colorResource(id = R.color.md_theme_tertiary)
        onStateColor == "colorError"    -> colorResource(id = R.color.md_theme_error)
        else                            -> colorResource(id = R.color.md_theme_primary)
    }
    val accentContentColor = lightContentFor(accentColor)

    val surfaceColor = colorResource(id = R.color.md_theme_surfaceVariant)
    val onSurfaceColor = colorResource(id = R.color.md_theme_onSurface)
    val contentColor = if (isEnabled) onSurfaceColor else onSurfaceColor.copy(alpha = 0.4f)

    val animatedContentColor by animateColorAsState(
        targetValue = contentColor,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "lightContentColor"
    )

    val (localBrightness, setLocalBrightness) = rememberDebouncedAction(
        initialValue = uiState.brightnessPercent,
        onSend = { v ->
            if (haClient == null) return@rememberDebouncedAction
            if (v <= 0) {
                haClient.callService("light", "turn_off", entity.id)
            } else {
                entity.lastKnownState?.set("last_brightness_pct", v)
                haClient.callService("light", "turn_on", entity.id, JSONObject().put("brightness_pct", v))
            }
        }
    )
    val adjustBrightness: (Int) -> Unit = { direction ->
        val next = (localBrightness + direction * BrightnessStepPercent).coerceIn(0, 100)
        if (next != localBrightness) setLocalBrightness(next)
    }

    val showBrightnessRow = supportsBrightness && "Brightness" in uiState.tabLabels
    val showWarmthRow = "Temperature" in uiState.tabLabels
    val showColorRow = "Color" in uiState.tabLabels

    val iconRes = remember(entity.id, entity.state, entity.customIconOnName, entity.customIconOffName) {
        EntityIconMapper.getFinalIconForEntity(entity) ?: R.drawable.ic_default
    }

    val savedEntitiesManager = remember { SavedEntitiesManager(context) }
    val handlePress: (PressType) -> Unit = remember(entity.id, haClient, savedEntitiesManager, isEntityInitialized) {
        { press ->
            if (isEntityInitialized) {
                EntityActionExecutor.perform(
                    entity = entity,
                    press = press,
                    haClient = haClient,
                    savedEntitiesManager = savedEntitiesManager,
                    onExpand = { expanded = true }
                )
            }
        }
    }

    // While expanded, BACK collapses the tile instead of closing the bar
    DisposableEffect(expanded) {
        if (expanded) {
            val handler = {
                expanded = false
                true
            }
            OverlayBackDispatcher.register(handler)
            onDispose { OverlayBackDispatcher.unregister(handler) }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            wasCardFocused.value = isFocused
            delay(50)
            bringIntoViewRequester.bringIntoView()
            delay(100)
            withContext(Dispatchers.Main.immediate) {
                try { firstRowFocusRequester.requestFocus() } catch (e: Exception) { Log.e("LightTile", "Focus request failed", e) }
            }
        } else if (wasCardFocused.value) {
            delay(100)
            withContext(Dispatchers.Main.immediate) {
                try {
                    cardFocusRequester.requestFocus()
                    wasCardFocused.value = false
                } catch (e: Exception) { Log.e("LightTile", "Focus request failed", e) }
            }
        }
    }

    val fillFraction = if (isEnabled && supportsBrightness && (isOn || localBrightness > 0)) {
        localBrightness / 100f
    } else 0f

    Card(
        modifier = modifier
            .focusRequester(cardFocusRequester)
            .tvFocusFrame(isFocused && !expanded, TileShape)
            .combinedClickable(
                enabled = !expanded && isEnabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = animatedContentColor),
                onClick = { handlePress(PressType.SINGLE) },
                onLongClick = { handlePress(PressType.LONG) }
            )
            .focusable(enabled = !expanded, interactionSource = interactionSource)
            .dpadAdjust(
                enabled = !expanded && isEnabled && supportsBrightness && adjustAxis != BarAdjustAxis.NONE,
                adjustVertically = adjustAxis == BarAdjustAxis.VERTICAL,
                onAdjust = adjustBrightness
            )
            .bringIntoViewRequester(bringIntoViewRequester),
        shape = TileShape,
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor,
            contentColor = animatedContentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.animateContentSize(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
        ) {
            // Value fill: live brightness shown behind the content
            if (!expanded && fillFraction > 0f) {
                Box(modifier = Modifier.matchParentSize()) {
                    if (isHorizontal) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .fillMaxHeight(fillFraction)
                                .background(accentColor.copy(alpha = 0.35f))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fillFraction)
                                .background(accentColor.copy(alpha = 0.35f))
                        )
                    }
                }
            }

            key(expanded) {
                if (expanded) {
                    ExpandedLightControls(
                        entity = entity,
                        haClient = haClient,
                        uiState = uiState,
                        isOn = isOn,
                        iconRes = iconRes,
                        accentColor = accentColor,
                        accentContentColor = accentContentColor,
                        contentColor = animatedContentColor,
                        backgroundColor = surfaceColor,
                        localBrightness = localBrightness,
                        onAdjustBrightness = adjustBrightness,
                        showBrightnessRow = showBrightnessRow,
                        showWarmthRow = showWarmthRow,
                        showColorRow = showColorRow,
                        firstRowFocusRequester = firstRowFocusRequester,
                        modifier = if (isHorizontal) Modifier.width(320.dp) else Modifier.fillMaxWidth()
                    )
                } else if (isHorizontal) {
                    HorizontalTileContent(
                        uiState = uiState,
                        isOn = isOn,
                        isEnabled = isEnabled,
                        isFocused = isFocused,
                        localBrightness = localBrightness,
                        canAdjust = supportsBrightness && adjustAxis != BarAdjustAxis.NONE,
                        supportsBrightness = supportsBrightness,
                        iconRes = iconRes,
                        accentColor = accentColor,
                        accentContentColor = accentContentColor,
                        contentColor = animatedContentColor
                    )
                } else {
                    VerticalTileContent(
                        entity = entity,
                        uiState = uiState,
                        isOn = isOn,
                        isEnabled = isEnabled,
                        isFocused = isFocused,
                        localBrightness = localBrightness,
                        canAdjust = supportsBrightness && adjustAxis != BarAdjustAxis.NONE,
                        supportsBrightness = supportsBrightness,
                        iconRes = iconRes,
                        accentColor = accentColor,
                        accentContentColor = accentContentColor,
                        contentColor = animatedContentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun IconCircle(
    iconRes: Int,
    name: String,
    active: Boolean,
    accentColor: Color,
    accentContentColor: Color,
    contentColor: Color,
    size: androidx.compose.ui.unit.Dp = 36.dp
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

private fun stateLine(isEnabled: Boolean, isOn: Boolean, supportsBrightness: Boolean, brightness: Int, rawState: String): String = when {
    !isEnabled -> rawState
    isOn && supportsBrightness -> "On · $brightness%"
    isOn -> "On"
    else -> "Off"
}

@Composable
private fun VerticalTileContent(
    entity: EntityItem,
    uiState: LightUiState,
    isOn: Boolean,
    isEnabled: Boolean,
    isFocused: Boolean,
    localBrightness: Int,
    canAdjust: Boolean,
    supportsBrightness: Boolean,
    iconRes: Int,
    accentColor: Color,
    accentContentColor: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(iconRes, uiState.name, isOn, accentColor, accentContentColor, contentColor)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = uiState.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stateLine(isEnabled, isOn, supportsBrightness, localBrightness, entity.state),
                fontSize = 12.sp,
                color = contentColor.copy(alpha = 0.65f)
            )
        }

        if (isFocused && isEnabled && canAdjust) {
            // Adjust affordance: ‹ value › while focused
            Text(text = "‹", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "$localBrightness%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Text(text = "›", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        } else if (uiState.indicatorColor != null) {
            ColorDot(color = uiState.indicatorColor, outline = contentColor, size = 14.dp)
        }
    }
}

@Composable
private fun HorizontalTileContent(
    uiState: LightUiState,
    isOn: Boolean,
    isEnabled: Boolean,
    isFocused: Boolean,
    localBrightness: Int,
    canAdjust: Boolean,
    supportsBrightness: Boolean,
    iconRes: Int,
    accentColor: Color,
    accentContentColor: Color,
    contentColor: Color
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .heightIn(min = 150.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconCircle(iconRes, uiState.name, isOn, accentColor, accentContentColor, contentColor)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = uiState.name,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (isFocused && isEnabled && canAdjust) {
            Text(text = "▴ $localBrightness% ▾", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        } else if (isOn && supportsBrightness) {
            Text(text = "$localBrightness%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * The in-place expansion shown after long-press: stacked control rows.
 * Up/down moves between rows, left/right adjusts the focused row, BACK collapses.
 */
@Composable
private fun ExpandedLightControls(
    entity: EntityItem,
    haClient: HomeAssistantClient?,
    uiState: LightUiState,
    isOn: Boolean,
    iconRes: Int,
    accentColor: Color,
    accentContentColor: Color,
    contentColor: Color,
    backgroundColor: Color,
    localBrightness: Int,
    onAdjustBrightness: (Int) -> Unit,
    showBrightnessRow: Boolean,
    showWarmthRow: Boolean,
    showColorRow: Boolean,
    firstRowFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val firstRow = when {
        showBrightnessRow -> "brightness"
        showWarmthRow -> "warmth"
        showColorRow -> "color"
        else -> ""
    }

    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(iconRes, uiState.name, isOn, accentColor, accentContentColor, contentColor, size = 32.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = uiState.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stateLine(true, isOn, showBrightnessRow, localBrightness, entity.state),
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.65f)
                )
            }
            PowerButton(
                isOn = isOn,
                onClick = { haClient?.callService("light", if (isOn) "turn_off" else "turn_on", entity.id) },
                contentColor = contentColor,
                backgroundColor = backgroundColor,
                size = 32.dp
            )
        }

        if (showBrightnessRow) {
            AdjustableControlRow(
                label = "BRIGHTNESS",
                valueText = "$localBrightness%",
                contentColor = contentColor,
                onAdjust = onAdjustBrightness,
                focusRequester = if (firstRow == "brightness") firstRowFocusRequester else null
            ) {
                ControlTrack(trackColor = contentColor.copy(alpha = 0.15f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((localBrightness / 100f).coerceIn(0f, 1f))
                            .background(accentColor)
                    )
                }
            }
        }

        if (showWarmthRow) {
            WarmthRow(
                entity = entity,
                haClient = haClient,
                uiState = uiState,
                contentColor = contentColor,
                focusRequester = if (firstRow == "warmth") firstRowFocusRequester else null
            )
        }

        if (showColorRow) {
            ColorRow(
                entity = entity,
                haClient = haClient,
                uiState = uiState,
                contentColor = contentColor,
                focusRequester = if (firstRow == "color") firstRowFocusRequester else null
            )
        }
    }
}

@Composable
private fun WarmthRow(
    entity: EntityItem,
    haClient: HomeAssistantClient?,
    uiState: LightUiState,
    contentColor: Color,
    focusRequester: FocusRequester?
) {
    val (localKelvin, setLocalKelvin) = rememberDebouncedAction(
        initialValue = uiState.currentKelvin.coerceIn(uiState.minKelvin, uiState.maxKelvin),
        onSend = { v -> haClient?.callService("light", "turn_on", entity.id, JSONObject().put("color_temp_kelvin", v)) }
    )
    val step = ((uiState.maxKelvin - uiState.minKelvin) / 10).coerceAtLeast(100)
    val fraction = if (uiState.maxKelvin > uiState.minKelvin) {
        (localKelvin - uiState.minKelvin).toFloat() / (uiState.maxKelvin - uiState.minKelvin).toFloat()
    } else 0f

    AdjustableControlRow(
        label = "WARMTH",
        valueText = "${localKelvin}K",
        contentColor = contentColor,
        onAdjust = { direction ->
            val next = (localKelvin + direction * step).coerceIn(uiState.minKelvin, uiState.maxKelvin)
            if (next != localKelvin) setLocalKelvin(next)
        },
        focusRequester = focusRequester
    ) {
        ControlTrack(
            trackBrush = Brush.horizontalGradient(
                listOf(Color(0xFFFF9800), Color(0xFFFFE0B2), Color(0xFFB3E5FC))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

private val ColorOptions = listOf(
    Color.White to "White", Color.Red to "Red", Color.Green to "Green", Color.Blue to "Blue",
    Color.Yellow to "Yellow", Color.Cyan to "Cyan", Color.Magenta to "Magenta", Color(0xFFFF7F00) to "Orange",
    Color(0xFF800080) to "Purple", Color(0xFF00FF7F) to "Spring Green"
)

@Composable
private fun ColorRow(
    entity: EntityItem,
    haClient: HomeAssistantClient?,
    uiState: LightUiState,
    contentColor: Color,
    focusRequester: FocusRequester?
) {
    fun closestIndex(c: Color): Int {
        var best = 0; var bestDist = Float.MAX_VALUE
        for (i in ColorOptions.indices) {
            val o = ColorOptions[i].first
            val d = (o.red - c.red)*(o.red - c.red) + (o.green - c.green)*(o.green - c.green) + (o.blue - c.blue)*(o.blue - c.blue)
            if (d < bestDist) { bestDist = d; best = i }
        }
        return best
    }

    val (localColorIndex, setLocalColorIndex) = rememberDebouncedAction(
        initialValue = closestIndex(uiState.rgbColor),
        onSend = { i ->
            val col = ColorOptions[i].first
            haClient?.callService(
                "light", "turn_on", entity.id,
                JSONObject().put("rgb_color", JSONArray().put((col.red * 255).toInt()).put((col.green * 255).toInt()).put((col.blue * 255).toInt()))
            )
        }
    )

    AdjustableControlRow(
        label = "COLOR",
        valueText = ColorOptions[localColorIndex].second,
        contentColor = contentColor,
        onAdjust = { direction ->
            setLocalColorIndex((localColorIndex + direction + ColorOptions.size) % ColorOptions.size)
        },
        focusRequester = focusRequester
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            ColorOptions.forEachIndexed { index, (color, _) ->
                val selected = index == localColorIndex
                Box(
                    modifier = Modifier
                        .size(if (selected) 16.dp else 12.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
                        )
                )
            }
        }
    }
}

/**
 * A focusable control row for the expanded tile. Left/right adjusts the value via
 * [onAdjust]; the row itself is one focus stop, so up/down moves between rows.
 */
@Composable
private fun AdjustableControlRow(
    label: String,
    valueText: String,
    contentColor: Color,
    onAdjust: (Int) -> Unit,
    focusRequester: FocusRequester?,
    track: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvFocusFrame(isFocused, ControlRowShape)
            .clip(ControlRowShape)
            .background(contentColor.copy(alpha = if (isFocused) 0.1f else 0.04f))
            .dpadAdjust(enabled = true, adjustVertically = false, onAdjust = onAdjust)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor.copy(alpha = if (isFocused) 0.85f else 0.6f)
            )
            Spacer(modifier = Modifier.height(5.dp))
            track()
        }
        if (valueText.isNotEmpty()) {
            Text(
                text = valueText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

@Composable
private fun ControlTrack(
    trackColor: Color? = null,
    trackBrush: Brush? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .then(
                when {
                    trackBrush != null -> Modifier.background(trackBrush)
                    trackColor != null -> Modifier.background(trackColor)
                    else -> Modifier
                }
            )
    ) {
        content()
    }
}

/**
 * A small circular visual indicator used to display the current color or temperature of a light.
 */
@Composable
private fun ColorDot(color: Color?, outline: Color, size: androidx.compose.ui.unit.Dp = 16.dp) {
    val dot = color ?: Color.Transparent
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(dot)
            .border(1.dp, outline.copy(alpha = 0.6f), CircleShape)
    )
}

/** Simple luma heuristic (sRGB) to select black/white for readability */
private fun lightContentFor(bg: Color): Color {
    val luma = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return if (luma > 0.6f) Color.Black else Color.White
}
