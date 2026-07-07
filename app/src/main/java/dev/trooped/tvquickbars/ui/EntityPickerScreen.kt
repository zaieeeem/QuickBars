package dev.trooped.tvquickbars.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.trooped.tvquickbars.R
import dev.trooped.tvquickbars.data.CategoryItem
import dev.trooped.tvquickbars.data.EntityItem

/**
 * Ten-foot, D-pad-first entity picker: a category rail on the left and the focused
 * category's entities on the right. Drives the existing [EntityImporterViewModel] — it
 * reads the category list out of [ImporterUiState.Success] and toggles selection through
 * the same methods the legacy screen used, so no data/logic changes are required.
 */

/** Curated, human-readable label + accent per Home Assistant domain. */
private data class DomainStyle(val label: String, val accent: Color)

private val DOMAIN_STYLES: Map<String, DomainStyle> = mapOf(
    "light" to DomainStyle("Lights", Color(0xFFFFC24B)),
    "switch" to DomainStyle("Switches", Color(0xFF5AC8FA)),
    "input_boolean" to DomainStyle("Toggles", Color(0xFF64D2FF)),
    "button" to DomainStyle("Buttons", Color(0xFF7D8CFF)),
    "input_button" to DomainStyle("Buttons", Color(0xFF7D8CFF)),
    "fan" to DomainStyle("Fans", Color(0xFF66E0C6)),
    "climate" to DomainStyle("Climate", Color(0xFFFF7A6B)),
    "cover" to DomainStyle("Covers & blinds", Color(0xFFB39DFF)),
    "lock" to DomainStyle("Locks", Color(0xFFFFB03A)),
    "alarm_control_panel" to DomainStyle("Alarm", Color(0xFFFF5C7A)),
    "camera" to DomainStyle("Cameras", Color(0xFF9AA6FF)),
    "media_player" to DomainStyle("Media players", Color(0xFF4FD1FF)),
    "scene" to DomainStyle("Scenes", Color(0xFFFF9F45)),
    "script" to DomainStyle("Scripts", Color(0xFF8FE388)),
    "automation" to DomainStyle("Automations", Color(0xFF8FE388)),
    "sensor" to DomainStyle("Sensors", Color(0xFF9FB2C4)),
    "binary_sensor" to DomainStyle("Binary sensors", Color(0xFF9FB2C4)),
)

private fun styleFor(domain: String): DomainStyle =
    DOMAIN_STYLES[domain.lowercase()]
        ?: DomainStyle(
            domain.replace('_', ' ').replaceFirstChar { it.uppercase() },
            Color(0xFF9FB2C4),
        )

private val Ground = Color(0xFF0E1116)
private val Panel = Color(0xFF161B22)
private val PanelFocused = Color(0xFF212A36)

@Composable
fun EntityPickerScreen(
    viewModel: EntityImporterViewModel,
    onNavigateToMain: () -> Unit,
    onNavigateBack: () -> Unit,
    onRetryConnection: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Ground)) {
        when (val state = uiState) {
            is ImporterUiState.Loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

            is ImporterUiState.Error ->
                EnhancedErrorView(state.title, state.message, onRetryConnection)

            is ImporterUiState.Success -> {
                val categories = remember(state.items) {
                    state.items.filterIsInstance<CategoryItem>()
                }
                if (categories.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(
                            "No entities found on this Home Assistant.",
                            color = Color(0xFF9FB2C4),
                            fontSize = 20.sp,
                        )
                    }
                } else {
                    TwoPanePicker(
                        categories = categories,
                        selectedCount = categories.sumOf { c -> c.entities.count { it.isSelected } },
                        onToggle = { entity, sel -> viewModel.onEntityChecked(entity, sel) },
                        onToggleCategory = { viewModel.onCategoryLongClicked(it) },
                        onContinue = onNavigateToMain,
                    )
                }
            }
        }
    }
}

@Composable
private fun TwoPanePicker(
    categories: List<CategoryItem>,
    selectedCount: Int,
    onToggle: (EntityItem, Boolean) -> Unit,
    onToggleCategory: (CategoryItem) -> Unit,
    onContinue: () -> Unit,
) {
    var focusedCategory by rememberSaveable { mutableIntStateOf(0) }
    val safeIndex = focusedCategory.coerceIn(0, categories.lastIndex)
    val current = categories[safeIndex]

    Column(Modifier.fillMaxSize().padding(40.dp)) {
        // Header: title + running selected count.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Choose what to control",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            SelectedPill(selectedCount)
        }
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxSize()) {
            // Left rail: categories.
            LazyColumn(
                modifier = Modifier.width(360.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(categories, key = { _, c -> c.name }) { index, cat ->
                    CategoryRailItem(
                        category = cat,
                        selected = cat.entities.count { it.isSelected },
                        active = index == safeIndex,
                        onFocused = { focusedCategory = index },
                    )
                }
            }

            Spacer(Modifier.width(28.dp))

            // Right pane: entities for the focused category, crossfading on change.
            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 })
                        .togetherWith(fadeOut(tween(140)))
                },
                label = "pane",
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) { cat ->
                EntityPane(
                    category = cat,
                    onToggle = onToggle,
                    onToggleCategory = onToggleCategory,
                    onContinue = onContinue,
                )
            }
        }
    }
}

@Composable
private fun SelectedPill(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(
            "$count selected",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CategoryRailItem(
    category: CategoryItem,
    selected: Int,
    active: Boolean,
    onFocused: () -> Unit,
) {
    val style = styleFor(category.name)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocused() }

    val bg by animateColorAsState(
        if (focused || active) PanelFocused else Panel, tween(160), label = "bg",
    )
    val scale by animateFloatAsState(if (focused) 1.03f else 1f, spring(), label = "sc")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) style.accent else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .focusable(interactionSource = interaction)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(style.accent.copy(alpha = 0.18f)),
            Alignment.Center,
        ) {
            Icon(
                painter = painterResource(EntityIconMapper.getIconForDomain(category.name)),
                contentDescription = null,
                tint = style.accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                style.label,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (selected > 0) "$selected of ${category.entities.size} on"
                else "${category.entities.size} available",
                color = if (selected > 0) style.accent else Color(0xFF8A97A6),
                fontSize = 14.sp,
            )
        }
        if (selected > 0) {
            Box(
                Modifier.size(10.dp).clip(CircleShape).background(style.accent),
            )
        }
    }
}

@Composable
private fun EntityPane(
    category: CategoryItem,
    onToggle: (EntityItem, Boolean) -> Unit,
    onToggleCategory: (CategoryItem) -> Unit,
    onContinue: () -> Unit,
) {
    val style = styleFor(category.name)
    val listState = rememberLazyListState()
    // Disambiguate identical friendly names (same name, different entity_id).
    val nameCounts = remember(category.entities) {
        category.entities.groupingBy { it.friendlyName.ifBlank { it.id } }.eachCount()
    }
    val allOn = category.entities.isNotEmpty() && category.entities.all { it.isSelected }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "__selectall") {
            SelectAllRow(allOn, style.accent) { onToggleCategory(category) }
        }
        itemsIndexed(category.entities, key = { _, e -> e.id }) { _, entity ->
            val label = entity.friendlyName.ifBlank { entity.id }
            val subtitle = if ((nameCounts[label] ?: 0) > 1) entity.id else null
            EntityCard(entity, label, subtitle, style.accent) { sel -> onToggle(entity, sel) }
        }
        item(key = "__continue") {
            Spacer(Modifier.height(8.dp))
            ContinueButton(onContinue)
        }
    }
}

@Composable
private fun SelectAllRow(allOn: Boolean, accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val bg by animateColorAsState(if (focused) PanelFocused else Color(0xFF11161D), tween(160), label = "sa")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(if (focused) 2.dp else 0.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(
            if (allOn) "Deselect all" else "Select all",
            color = accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(if (allOn) "on" else "", color = accent, fontSize = 14.sp)
    }
}

@Composable
private fun EntityCard(
    entity: EntityItem,
    label: String,
    subtitle: String?,
    accent: Color,
    onToggle: (Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val selected = entity.isSelected

    val bg by animateColorAsState(if (focused) PanelFocused else Panel, tween(160), label = "ec")
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, spring(), label = "es")
    val checkScale by animateFloatAsState(if (selected) 1f else 0f, spring(), label = "chk")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(if (focused) 2.dp else 0.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null) { onToggle(!selected) }
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(subtitle, color = Color(0xFF6E7B8A), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(16.dp))
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color.Transparent)
                .border(2.dp, if (selected) accent else Color(0xFF5A6675), CircleShape),
            Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = if (selected) "Selected" else "Not selected",
                tint = Color(0xFF0E1116),
                modifier = Modifier.size(18.dp).scale(checkScale),
            )
        }
    }
}

@Composable
private fun ContinueButton(onContinue: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val elevation by animateDpAsState(if (focused) 6.dp else 0.dp, tween(160), label = "cont")

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(top = elevation)
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
            .clickable(interactionSource = interaction, indication = null, onClick = onContinue),
    ) {
        Text(
            "Continue",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
