package dev.trooped.tvquickbars.ui.QuickBar.entities.cards.media_player

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.trooped.tvquickbars.R
import dev.trooped.tvquickbars.data.EntityItem
import dev.trooped.tvquickbars.data.PressType
import dev.trooped.tvquickbars.ha.HomeAssistantClient
import dev.trooped.tvquickbars.persistence.SavedEntitiesManager
import dev.trooped.tvquickbars.ui.AnimatedControlButton
import dev.trooped.tvquickbars.ui.EntityIconMapper
import dev.trooped.tvquickbars.ui.QuickBar.controls.PowerButton
import dev.trooped.tvquickbars.ui.QuickBar.foundation.AlphaLow
import dev.trooped.tvquickbars.ui.QuickBar.foundation.AlphaMedium
import dev.trooped.tvquickbars.ui.QuickBar.foundation.BarAdjustAxis
import dev.trooped.tvquickbars.ui.QuickBar.foundation.LocalBarAdjustAxis
import dev.trooped.tvquickbars.ui.QuickBar.foundation.OverlayBackDispatcher
import dev.trooped.tvquickbars.ui.QuickBar.foundation.TileIconCircle
import dev.trooped.tvquickbars.ui.QuickBar.foundation.TvTileShape
import dev.trooped.tvquickbars.ui.QuickBar.foundation.accentContentFor
import dev.trooped.tvquickbars.ui.QuickBar.foundation.dpadAdjust
import dev.trooped.tvquickbars.ui.QuickBar.foundation.rememberDebouncedAction
import dev.trooped.tvquickbars.ui.QuickBar.foundation.resolveTileAccent
import dev.trooped.tvquickbars.ui.QuickBar.foundation.tvFocusFrame
import dev.trooped.tvquickbars.utils.EntityActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.text.ifEmpty

private const val VolumeStepPercent = 5

/**
 * UI state representation for a Media Player entity.
 *
 * This data class encapsulates all the necessary properties required to render a media player card,
 * including its current playback status, connectivity state, and visual metadata.
 *
 * @property name The display name of the media player (either a custom alias or the friendly name).
 * @property state The raw state string received from the Home Assistant entity (e.g., "playing", "paused", "off").
 * @property isOn Indicates whether the media player is currently in an active power state.
 * @property isEnabled Indicates if the entity is available and not in an "unknown" or "unavailable" state.
 * @property isPlaying Specifically indicates if the current state is "playing".
 * @property isMuted Indicates if the volume is currently muted based on the entity attributes.
 * @property iconRes The resource ID of the icon to be displayed, determined by the entity's type and state.
 * @property volumePercent Current volume 0-100, or null when the player does not report one.
 * @property canSetVolume Whether the player supports direct volume setting (VOLUME_SET feature).
 */
data class MediaPlayerUiState(
    val name: String,
    val state: String,
    val isOn: Boolean,
    val isEnabled: Boolean,
    val isPlaying: Boolean,
    val isMuted: Boolean,
    val iconRes: Int,
    val volumePercent: Int?,
    val canSetVolume: Boolean
)

/**
 * Remembers and maps the current state of a media player entity into a [MediaPlayerUiState].
 *
 * This composable handles the logic for determining the display name, operational state (on/off),
 * playback status, mute status, and the appropriate icon based on the [EntityItem] attributes.
 *
 * @param entity The media player entity data from Home Assistant.
 * @return A [MediaPlayerUiState] containing the processed UI properties.
 */
@Composable
fun rememberMediaPlayerUiState(entity: EntityItem): MediaPlayerUiState {
    val name = remember(entity.customName, entity.friendlyName) {
        entity.customName.ifEmpty { entity.friendlyName }
    }
    val state = entity.state
    val isEnabled = state !in listOf("unavailable", "unknown")
    val isOn = remember(state) { isMediaOn(state) }
    val isPlaying = remember(state) { isMediaPlaying(state) }
    val isMuted = remember(entity.attributes) { isMediaMuted(entity) }
    val volumePercent = remember(entity.attributes) { mediaVolumePercent(entity) }
    val canSetVolume = remember(entity.attributes) { supportsVolumeSet(entity) }

    val iconRes = remember(entity.id, entity.state, entity.customIconOnName, entity.customIconOffName) {
        EntityIconMapper.getFinalIconForEntity(entity) ?: R.drawable.ic_default
    }

    return MediaPlayerUiState(
        name = name, state = state, isOn = isOn, isEnabled = isEnabled,
        isPlaying = isPlaying, isMuted = isMuted, iconRes = iconRes,
        volumePercent = volumePercent, canSetVolume = canSetVolume
    )
}

/**
 * A composable function that represents a media player entity as a card in the QuickBar UI.
 *
 * The card follows the overlay tile system: while the tile is focused, the free D-pad axis
 * adjusts the volume directly (the tile background fills to show the level), OK performs the
 * configured press action, long-press expands the playback controls in place, and BACK
 * collapses the expanded panel before closing the bar.
 *
 * @param entity The [EntityItem] data object containing the media player's state and attributes.
 * @param haClient The [HomeAssistantClient] used to dispatch service calls for media control.
 * @param onStateColor A string identifier representing the color to be used when the entity is "on".
 * @param customOnStateColor An optional list of RGB integers used if [onStateColor] is set to "custom".
 * @param modifier The [Modifier] to be applied to the card's layout.
 * @param isHorizontal A boolean flag determining if the card should be rendered in a horizontal aspect ratio.
 * @param isEntityInitialized A boolean flag indicating if the entity data is fully loaded and ready for interaction.
 */
@Composable
fun MediaPlayerEntityCard(
    entity: EntityItem,
    haClient: HomeAssistantClient?,
    onStateColor: String,
    customOnStateColor: List<Int>? = null,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false,
    isEntityInitialized: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    val adjustAxis = LocalBarAdjustAxis.current
    val cardFocusRequester = remember { FocusRequester() }
    val closeBtnFocusRequester = remember { FocusRequester() }

    val wasCardFocused = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) wasCardFocused.value = true
    }

    // While expanded, BACK collapses the panel instead of closing the bar
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
                try { closeBtnFocusRequester.requestFocus() } catch (e: Exception) { Log.e("MediaPlayerEntityCard", "Focus request failed", e) }
            }
        } else if (wasCardFocused.value) {
            delay(100)
            withContext(Dispatchers.Main.immediate) {
                try {
                    cardFocusRequester.requestFocus()
                    wasCardFocused.value = false
                } catch (e: Exception) { Log.e("MediaPlayerEntityCard", "Focus request failed", e) }
            }
        }
    }

    val uiState = rememberMediaPlayerUiState(entity)

    val accentColor = resolveTileAccent(entity.id, onStateColor, customOnStateColor)
    val accentContentColor = accentContentFor(accentColor)

    val surfaceColor = colorResource(id = R.color.md_theme_surfaceVariant)
    val onSurfaceColor = colorResource(id = R.color.md_theme_onSurface)
    val disabledContentColor = onSurfaceColor.copy(alpha = 0.5f)

    val animatedContentColor by animateColorAsState(
        targetValue = if (uiState.isEnabled) onSurfaceColor else disabledContentColor,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "mp_fg"
    )

    val context = LocalContext.current
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

    // Direct D-pad volume: local value updates instantly, sends are debounced.
    val (localVolume, setLocalVolume) = rememberDebouncedAction(
        initialValue = uiState.volumePercent ?: 0,
        onSend = { v ->
            if (haClient == null) return@rememberDebouncedAction
            haClient.callService(
                "media_player", "volume_set", entity.id,
                JSONObject().put("volume_level", v / 100.0)
            )
        }
    )
    val canAdjustVolume = uiState.canSetVolume && uiState.volumePercent != null &&
            uiState.isOn && uiState.isEnabled && adjustAxis != BarAdjustAxis.NONE
    val adjustVolume: (Int) -> Unit = { direction ->
        val next = (localVolume + direction * VolumeStepPercent).coerceIn(0, 100)
        if (next != localVolume) setLocalVolume(next)
    }

    val fillFraction = if (canAdjustVolume) localVolume / 100f else 0f

    Card(
        modifier = modifier
            .focusRequester(cardFocusRequester)
            .tvFocusFrame(isFocused && !expanded, TvTileShape)
            .combinedClickable(
                enabled = !expanded && uiState.isEnabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = animatedContentColor),
                onClick = { handlePress(PressType.SINGLE) },
                onLongClick = { handlePress(PressType.LONG) }
            )
            .focusable(enabled = !expanded, interactionSource = interactionSource)
            .dpadAdjust(
                enabled = !expanded && canAdjustVolume,
                adjustVertically = adjustAxis == BarAdjustAxis.VERTICAL,
                onAdjust = adjustVolume
            )
            .bringIntoViewRequester(bringIntoViewRequester),
        shape = TvTileShape,
        colors = CardDefaults.cardColors(containerColor = surfaceColor, contentColor = animatedContentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .animateContentSize(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
                .fillMaxSize()
        ) {
            // Value fill: live volume shown behind the content
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
                if (!isHorizontal) {
                    VerticalMediaContent(
                        entity = entity,
                        uiState = uiState,
                        haClient = haClient,
                        expanded = expanded,
                        onClose = { expanded = false },
                        contentColor = animatedContentColor,
                        backgroundColor = surfaceColor,
                        accentColor = accentColor,
                        accentContentColor = accentContentColor,
                        isFocused = isFocused,
                        canAdjustVolume = canAdjustVolume,
                        localVolume = localVolume,
                        closeBtnFocusRequester = closeBtnFocusRequester
                    )
                } else {
                    HorizontalMediaContent(
                        entity = entity,
                        uiState = uiState,
                        haClient = haClient,
                        expanded = expanded,
                        onClose = { expanded = false },
                        contentColor = animatedContentColor,
                        backgroundColor = surfaceColor,
                        accentColor = accentColor,
                        accentContentColor = accentContentColor,
                        isFocused = isFocused,
                        canAdjustVolume = canAdjustVolume,
                        localVolume = localVolume,
                        closeBtnFocusRequester = closeBtnFocusRequester
                    )
                }
            }
        }
    }
}

private fun mediaStateLine(uiState: MediaPlayerUiState, localVolume: Int): String {
    val base = when {
        !uiState.isEnabled -> uiState.state.replaceFirstChar { it.uppercase() }
        else -> uiState.state.replaceFirstChar { it.uppercase() }
    }
    return if (uiState.isOn && uiState.isEnabled && uiState.canSetVolume && uiState.volumePercent != null) {
        if (uiState.isMuted) "$base · Muted" else "$base · $localVolume%"
    } else base
}

/**
 * Renders the interactive control buttons for the media player, including volume
 * adjustments (up, down, mute) and playback controls (previous, play/pause, next).
 *
 */
@Composable
private fun MediaControlsSection(
    entity: EntityItem,
    uiState: MediaPlayerUiState,
    haClient: HomeAssistantClient?,
    contentColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaCtlButton(
                R.drawable.volume_down_24px, "Volume down",
                { haClient?.callService("media_player", "volume_down", entity.id, null) },
                contentColor, backgroundColor
            )

            MediaCtlButton(
                if (uiState.isMuted) R.drawable.volume_off_24px else R.drawable.volume_mute_24px, "Mute",
                {
                    val data = JSONObject().apply { put("is_volume_muted", !uiState.isMuted) }
                    haClient?.callService("media_player", "volume_mute", entity.id, data)
                },
                contentColor, backgroundColor, isSelected = uiState.isMuted
            )

            MediaCtlButton(
                R.drawable.volume_up_24px, "Volume up",
                { haClient?.callService("media_player", "volume_up", entity.id, null) },
                contentColor, backgroundColor
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaCtlButton(
                R.drawable.skip_previous_24px, "Previous",
                { haClient?.callService("media_player", "media_previous_track", entity.id, null) },
                contentColor, backgroundColor
            )

            MediaCtlButton(
                if (uiState.isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px, "Play/Pause",
                { haClient?.callService("media_player", "media_play_pause", entity.id, null) },
                contentColor, backgroundColor, isSelected = uiState.isPlaying, size = 44.dp
            )

            MediaCtlButton(
                R.drawable.skip_next_24px, "Next",
                { haClient?.callService("media_player", "media_next_track", entity.id, null) },
                contentColor, backgroundColor
            )
        }
    }
}

@Composable
private fun VerticalMediaContent(
    entity: EntityItem,
    uiState: MediaPlayerUiState,
    haClient: HomeAssistantClient?,
    expanded: Boolean,
    onClose: () -> Unit,
    contentColor: Color,
    backgroundColor: Color,
    accentColor: Color,
    accentContentColor: Color,
    isFocused: Boolean,
    canAdjustVolume: Boolean,
    localVolume: Int,
    closeBtnFocusRequester: FocusRequester
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TileIconCircle(
                iconRes = uiState.iconRes,
                name = uiState.name,
                active = uiState.isOn && uiState.isEnabled,
                accentColor = accentColor,
                accentContentColor = accentContentColor,
                contentColor = contentColor
            )

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
                    text = mediaStateLine(uiState, localVolume),
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.65f)
                )
            }

            if (!expanded && isFocused && canAdjustVolume) {
                // Adjust affordance: ‹ value › while focused
                Text(text = "‹", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "$localVolume%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                Text(text = "›", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = contentColor.copy(alpha = 0.2f))
                Spacer(Modifier.height(8.dp))

                MediaControlsSection(entity = entity, uiState = uiState, haClient = haClient, contentColor = contentColor, backgroundColor = backgroundColor)

                Spacer(Modifier.height(10.dp))
                PowerButton(contentColor = contentColor, backgroundColor = backgroundColor, onClick = { haClient?.callService("media_player", "toggle", entity.id) }, isOn = uiState.isOn)

                Spacer(Modifier.height(8.dp))
                CloseCircleButton(
                    focusRequester = closeBtnFocusRequester,
                    contentColor = contentColor,
                    backgroundColor = backgroundColor,
                    onClose = onClose,
                    size = 40.dp
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun HorizontalMediaContent(
    entity: EntityItem,
    uiState: MediaPlayerUiState,
    haClient: HomeAssistantClient?,
    expanded: Boolean,
    onClose: () -> Unit,
    contentColor: Color,
    backgroundColor: Color,
    accentColor: Color,
    accentContentColor: Color,
    isFocused: Boolean,
    canAdjustVolume: Boolean,
    localVolume: Int,
    closeBtnFocusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .heightIn(min = 150.dp)
            .then(if (expanded) Modifier.width(360.dp) else Modifier.width(120.dp))
            .clip(TvTileShape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.width(104.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TileIconCircle(
                iconRes = uiState.iconRes,
                name = uiState.name,
                active = uiState.isOn && uiState.isEnabled,
                accentColor = accentColor,
                accentContentColor = accentContentColor,
                contentColor = contentColor
            )
            Spacer(Modifier.height(6.dp))
            Text(text = uiState.name, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 2.dp))
            if (!expanded && isFocused && canAdjustVolume) {
                Text(text = "▴ $localVolume% ▾", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else if (uiState.state.isNotBlank()) {
                Text(
                    text = mediaStateLine(uiState, localVolume),
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (expanded) {
            Box(Modifier.width(1.dp).height(100.dp).background(contentColor.copy(alpha = 0.2f)))

            Box(Modifier.weight(1f).fillMaxHeight()) {
                MediaControlsSection(
                    entity = entity, uiState = uiState, haClient = haClient, contentColor = contentColor, backgroundColor = backgroundColor,
                    modifier = Modifier.fillMaxSize().padding(end = 48.dp)
                )

                Column(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PowerButton(contentColor = contentColor, backgroundColor = backgroundColor, onClick = { haClient?.callService("media_player", "toggle", entity.id) }, isOn = uiState.isOn, size = 36.dp)

                    CloseCircleButton(
                        focusRequester = closeBtnFocusRequester,
                        contentColor = contentColor,
                        backgroundColor = backgroundColor,
                        onClose = onClose,
                        size = 36.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun CloseCircleButton(
    focusRequester: FocusRequester,
    contentColor: Color,
    backgroundColor: Color,
    onClose: () -> Unit,
    size: Dp
) {
    val closeIsrc = remember { MutableInteractionSource() }
    val closeFocused by closeIsrc.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .size(size)
            .clip(CircleShape)
            .background(if (closeFocused) contentColor else contentColor.copy(alpha = AlphaLow))
            .border(width = if (closeFocused) 2.dp else 1.dp, color = if (closeFocused) contentColor else contentColor.copy(alpha = AlphaMedium), shape = CircleShape)
            .clickable(interactionSource = closeIsrc, indication = ripple(), onClick = onClose)
            .focusable(interactionSource = closeIsrc),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = if (closeFocused) backgroundColor else contentColor, modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
private fun MediaCtlButton(
    @DrawableRes icon: Int,
    contentDesc: String,
    onClick: () -> Unit,
    contentColor: Color,
    backgroundColor: Color,
    isSelected: Boolean = false,
    size: Dp = 40.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    AnimatedControlButton(
        onClick = onClick,
        contentColor = contentColor,
        backgroundColor = backgroundColor,
        modifier = Modifier,
        size = size,
        isSelected = isSelected,
        interactionSource = interactionSource
    ) { isFocused, _, animationScale, contentColor, backgroundColor ->
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDesc,
            tint = if (isFocused) backgroundColor else contentColor,
            modifier = Modifier.size(size * 0.55f).scale(animationScale)
        )
    }
}
