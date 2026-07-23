package by.vsdev.tablet.demo.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
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
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clearAndSetSemantics { },
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
) {
    val cellColors = LocalCellColors.current
    val appHaptics = LocalAppHaptics.current
    val background = if (selected) cellColors.selected else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) cellColors.onSelected else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) cellColors.selected else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 3.dp else 1.dp

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(cellHeight)
                .clip(MaterialTheme.shapes.small)
                .background(background)
                .border(borderWidth, borderColor, MaterialTheme.shapes.small)
                .semantics {
                    contentDescription = cellDescription
                    collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
                    this.selected = selected
                    stateDescription = if (selected) selectedDescription else notSelectedDescription
                    customActions =
                        listOf(
                            CustomAccessibilityAction(editLabel) {
                                appHaptics.performCellEdit()
                                onDoubleClick()
                                true
                            },
                        )
                }.onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.F2)) {
                        appHaptics.performCellEdit()
                        onDoubleClick()
                        true
                    } else {
                        false
                    }
                }.combinedClickable(
                    onClickLabel = toggleLabel,
                    onClick = {
                        appHaptics.performCellSelection(isSelected = !selected)
                        onClick()
                    },
                    onDoubleClick = {
                        appHaptics.performCellEdit()
                        onDoubleClick()
                    },
                ).padding(horizontal = AppSpacing.small),
        contentAlignment = Alignment.Center,
    ) {
        CellText(text = text, color = contentColor)
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
