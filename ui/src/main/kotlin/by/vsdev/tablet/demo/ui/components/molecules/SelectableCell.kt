package by.vsdev.tablet.demo.ui.components.molecules

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import by.vsdev.tablet.demo.ui.haptics.LocalAppHaptics
import by.vsdev.tablet.demo.ui.theme.AppSpacing
import by.vsdev.tablet.demo.ui.theme.AppTheme
import by.vsdev.tablet.demo.ui.theme.LocalCellColors

private val MinimumSelectableCellHeight = 56.dp

private data class SelectableCellStyle(
    val background: Color,
    val content: Color,
    val border: Color,
    val borderWidth: Dp,
)

internal fun shouldShowCellFocusIndicator(
    focused: Boolean,
    inputMode: InputMode,
): Boolean = focused && inputMode == InputMode.Keyboard

@Composable
private fun selectableCellStyle(
    selected: Boolean,
    focused: Boolean,
): SelectableCellStyle {
    val cellColors = LocalCellColors.current
    return SelectableCellStyle(
        background = if (selected) cellColors.selected else MaterialTheme.colorScheme.surfaceVariant,
        content = if (selected) cellColors.onSelected else MaterialTheme.colorScheme.onSurfaceVariant,
        border =
            when {
                focused -> MaterialTheme.colorScheme.primary
                selected -> cellColors.selected
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        borderWidth =
            when {
                focused -> 4.dp
                selected -> 3.dp
                else -> 1.dp
            },
    )
}

@Composable
internal fun selectableCellHeight(): Dp {
    val textHeight =
        with(LocalDensity.current) {
            MaterialTheme.typography.bodyMedium.lineHeight
                .toDp()
        }
    return maxOf(MinimumSelectableCellHeight, textHeight + AppSpacing.medium)
}

@Composable
private fun CellText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.clearAndSetSemantics { },
    )
}

@Composable
internal fun SelectableCell(
    text: String,
    selected: Boolean,
    row: Int,
    column: Int,
    cellDescription: String,
    selectedDescription: String,
    notSelectedDescription: String,
    toggleLabel: String,
    editLabel: String,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier,
    cellHeight: Dp = selectableCellHeight(),
    interactionSource: MutableInteractionSource? = null,
) {
    val appHaptics = LocalAppHaptics.current
    val inputMode = LocalInputModeManager.current.inputMode
    val defaultInteractionSource = remember { MutableInteractionSource() }
    val resolvedInteractionSource = interactionSource ?: defaultInteractionSource
    val indication = LocalIndication.current
    var isFocused by remember { mutableStateOf(false) }
    val style =
        selectableCellStyle(
            selected = selected,
            focused = shouldShowCellFocusIndicator(isFocused, inputMode),
        )
    val selectCell = {
        appHaptics.performCellSelection(isSelected = !selected)
        onClick()
    }
    val editCell = {
        appHaptics.performCellEdit()
        onDoubleClick()
    }
    val currentSelectCell by rememberUpdatedState(selectCell)
    val currentEditCell by rememberUpdatedState(editCell)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(cellHeight)
                .clip(MaterialTheme.shapes.small)
                .background(style.background)
                .border(style.borderWidth, style.border, MaterialTheme.shapes.small)
                .onFocusChanged { isFocused = it.isFocused }
                .semantics {
                    contentDescription = cellDescription
                    collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
                    this.selected = selected
                    stateDescription = if (selected) selectedDescription else notSelectedDescription
                }.cellAccessibilityActions(toggleLabel, editLabel, selectCell, editCell)
                .cellKeyboardActions(selectCell, editCell)
                .focusable()
                .indication(resolvedInteractionSource, indication)
                .cellTouchActions(
                    interactionSource = resolvedInteractionSource,
                    onSelect = { currentSelectCell() },
                    onEdit = { currentEditCell() },
                ).padding(horizontal = AppSpacing.small),
        contentAlignment = Alignment.Center,
    ) {
        CellText(
            text = text,
            color = style.content,
        )
    }
}

private fun Modifier.cellAccessibilityActions(
    toggleLabel: String,
    editLabel: String,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
): Modifier =
    semantics {
        customActions =
            listOf(
                CustomAccessibilityAction(editLabel) {
                    onEdit()
                    true
                },
            )
        onClick(label = toggleLabel) {
            onSelect()
            true
        }
    }

private fun Modifier.cellTouchActions(
    interactionSource: MutableInteractionSource,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
): Modifier =
    pointerInput(interactionSource) {
        var pendingPress: PressInteraction.Press? = null

        fun finishPress(released: Boolean) {
            val press = pendingPress ?: return
            pendingPress = null
            interactionSource.tryEmit(
                if (released) {
                    PressInteraction.Release(press)
                } else {
                    PressInteraction.Cancel(press)
                },
            )
        }

        try {
            detectTapGestures(
                onPress = { offset ->
                    if (pendingPress == null) {
                        PressInteraction.Press(offset).also { press ->
                            pendingPress = press
                            interactionSource.emit(press)
                        }
                    }

                    if (!tryAwaitRelease()) {
                        finishPress(released = false)
                    }
                },
                onTap = {
                    onSelect()
                    finishPress(released = true)
                },
                onDoubleTap = {
                    finishPress(released = true)
                    onEdit()
                },
            )
        } finally {
            finishPress(released = false)
        }
    }

private fun Modifier.cellKeyboardActions(
    onSelect: () -> Unit,
    onEdit: () -> Unit,
): Modifier =
    onPreviewKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyUp && event.key == Key.F2 -> {
                onEdit()
                true
            }

            event.type == KeyEventType.KeyUp &&
                (event.key == Key.Enter || event.key == Key.DirectionCenter) -> {
                onSelect()
                true
            }

            event.type == KeyEventType.KeyDown &&
                (event.key == Key.Enter || event.key == Key.DirectionCenter || event.key == Key.F2) -> true

            else -> false
        }
    }

@Preview
@Composable
private fun SelectableCellPreview() {
    AppTheme {
        SelectableCell(
            text = "aB3xK9",
            selected = false,
            row = 0,
            column = 0,
            cellDescription = "Row 1, column 1: aB3xK9",
            selectedDescription = "Selected",
            notSelectedDescription = "Not selected",
            toggleLabel = "Toggle selection",
            editLabel = "Edit cell",
            onClick = {},
            onDoubleClick = {},
        )
    }
}

@Preview
@Composable
private fun SelectableCellSelectedPreview() {
    AppTheme {
        SelectableCell(
            text = "aB3xK9",
            selected = true,
            row = 0,
            column = 0,
            cellDescription = "Row 1, column 1: aB3xK9",
            selectedDescription = "Selected",
            notSelectedDescription = "Not selected",
            toggleLabel = "Toggle selection",
            editLabel = "Edit cell",
            onClick = {},
            onDoubleClick = {},
        )
    }
}
