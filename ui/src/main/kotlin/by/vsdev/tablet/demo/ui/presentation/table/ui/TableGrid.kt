package by.vsdev.tablet.demo.ui.presentation.table.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import by.vsdev.tablet.demo.ui.R
import by.vsdev.tablet.demo.ui.components.molecules.SelectableCell
import by.vsdev.tablet.demo.ui.components.molecules.selectableCellHeight
import by.vsdev.tablet.demo.ui.presentation.table.CellUiState
import by.vsdev.tablet.demo.ui.presentation.table.TableIntent
import by.vsdev.tablet.demo.ui.presentation.table.TableLoadState
import by.vsdev.tablet.demo.ui.theme.AppSpacing
import by.vsdev.tablet.demo.ui.theme.AppTheme
import kotlinx.collections.immutable.toPersistentList

private val TableContentPadding = AppSpacing.medium
private val TableCellSpacing = AppSpacing.small
private val ScrollIndicatorThickness = 16.dp
private val MinimumCellWidth = 112.dp
private val MaximumCellWidth = 220.dp

private object TableCellContentType

internal data class ScrollThumbGeometry(
    val offset: Float,
    val length: Float,
)

internal fun calculateScrollThumbGeometry(
    trackLength: Float,
    viewportLength: Float,
    contentLength: Float,
    scrollOffset: Float,
    minimumThumbLength: Float,
): ScrollThumbGeometry? {
    if (trackLength <= 0f || viewportLength <= 0f || contentLength <= viewportLength) return null

    val thumbLength =
        (trackLength * viewportLength / contentLength)
            .coerceIn(minimumThumbLength.coerceAtMost(trackLength), trackLength)
    val maximumScrollOffset = contentLength - viewportLength
    val scrollFraction = scrollOffset.coerceIn(0f, maximumScrollOffset) / maximumScrollOffset
    return ScrollThumbGeometry(
        offset = (trackLength - thumbLength) * scrollFraction,
        length = thumbLength,
    )
}

internal data class ScrollIndicatorVisibility(
    val vertical: Boolean,
    val horizontal: Boolean,
)

internal fun calculateScrollIndicatorVisibility(
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
    indicatorThickness: Float,
): ScrollIndicatorVisibility {
    var vertical = contentHeight > viewportHeight
    var horizontal = contentWidth > viewportWidth

    repeat(2) {
        horizontal = contentWidth > (viewportWidth - if (vertical) indicatorThickness else 0f)
        vertical = contentHeight > (viewportHeight - if (horizontal) indicatorThickness else 0f)
    }

    return ScrollIndicatorVisibility(vertical = vertical, horizontal = horizontal)
}

private data class TableGridLayout(
    val indicatorVisibility: ScrollIndicatorVisibility,
    val endInset: Dp,
    val bottomInset: Dp,
    val tableWidth: Dp,
)

private fun calculateTableGridLayout(
    viewportWidth: Dp,
    viewportHeight: Dp,
    columns: Int,
    itemCount: Int,
    cellHeight: Dp,
): TableGridLayout {
    val totalRows = (itemCount + columns - 1) / columns
    val contentHeight =
        TableContentPadding * 2 +
            cellHeight * totalRows +
            TableCellSpacing * (totalRows - 1).coerceAtLeast(0)
    val minimumTableWidth =
        TableContentPadding * 2 +
            MinimumCellWidth * columns +
            TableCellSpacing * (columns - 1)
    val indicatorVisibility =
        calculateScrollIndicatorVisibility(
            viewportWidth = viewportWidth.value,
            viewportHeight = viewportHeight.value,
            contentWidth = minimumTableWidth.value,
            contentHeight = contentHeight.value,
            indicatorThickness = ScrollIndicatorThickness.value,
        )
    val endInset = if (indicatorVisibility.vertical) ScrollIndicatorThickness else 0.dp
    val bottomInset = if (indicatorVisibility.horizontal) ScrollIndicatorThickness else 0.dp
    val availableWidth = (viewportWidth - endInset).coerceAtLeast(0.dp)
    val availableCellWidth =
        (availableWidth - TableContentPadding * 2 - TableCellSpacing * (columns - 1)) / columns
    val cellWidth = availableCellWidth.coerceIn(MinimumCellWidth, MaximumCellWidth)
    val tableWidth =
        TableContentPadding * 2 + cellWidth * columns + TableCellSpacing * (columns - 1)

    return TableGridLayout(
        indicatorVisibility = indicatorVisibility,
        endInset = endInset,
        bottomInset = bottomInset,
        tableWidth = tableWidth,
    )
}

@Composable
internal fun TableGrid(
    columns: Int,
    content: TableLoadState.Content,
    onIntent: (TableIntent) -> Unit,
    modifier: Modifier = Modifier,
    restoreFocusIndex: Int? = null,
) {
    val cells = content.cells
    val horizontalScrollState = rememberScrollState()
    val verticalGridState = rememberLazyGridState()
    val cellHeight = selectableCellHeight()
    val cellFocusRequester = remember { FocusRequester() }
    var placedRestoreIndex by remember { mutableStateOf<Int?>(null) }

    RestoreCellFocus(
        index = restoreFocusIndex,
        placedIndex = placedRestoreIndex,
        cellCount = cells.size,
        gridState = verticalGridState,
        focusRequester = cellFocusRequester,
        onScrollStarted = { placedRestoreIndex = null },
    )

    Surface(
        modifier = modifier.fillMaxSize().padding(AppSpacing.medium),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        TableGridViewport(
            columns = columns,
            content = content,
            cellHeight = cellHeight,
            verticalGridState = verticalGridState,
            horizontalScrollState = horizontalScrollState,
            restoreFocusIndex = restoreFocusIndex,
            cellFocusRequester = cellFocusRequester,
            onRestoreTargetPlaced = { placedRestoreIndex = it },
            onIntent = onIntent,
        )
    }
}

@Composable
private fun TableGridViewport(
    columns: Int,
    content: TableLoadState.Content,
    cellHeight: Dp,
    verticalGridState: LazyGridState,
    horizontalScrollState: ScrollState,
    restoreFocusIndex: Int?,
    cellFocusRequester: FocusRequester,
    onRestoreTargetPlaced: (Int) -> Unit,
    onIntent: (TableIntent) -> Unit,
) {
    val cells = content.cells
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layout =
            calculateTableGridLayout(
                viewportWidth = maxWidth,
                viewportHeight = maxHeight,
                columns = columns,
                itemCount = cells.size,
                cellHeight = cellHeight,
            )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(end = layout.endInset, bottom = layout.bottomInset)
                    .horizontalScroll(horizontalScrollState),
        ) {
            TableCells(
                columns = columns,
                content = content,
                cellHeight = cellHeight,
                state = verticalGridState,
                restoreFocusIndex = restoreFocusIndex,
                cellFocusRequester = cellFocusRequester,
                onRestoreTargetPlaced = onRestoreTargetPlaced,
                onIntent = onIntent,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .width(layout.tableWidth)
                        .fillMaxHeight(),
            )
        }
        TableScrollIndicators(
            layout = layout,
            columns = columns,
            cellCount = cells.size,
            cellHeight = cellHeight,
            verticalGridState = verticalGridState,
            horizontalScrollState = horizontalScrollState,
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.TableScrollIndicators(
    layout: TableGridLayout,
    columns: Int,
    cellCount: Int,
    cellHeight: Dp,
    verticalGridState: LazyGridState,
    horizontalScrollState: ScrollState,
) {
    if (layout.indicatorVisibility.vertical) {
        VerticalGridScrollIndicator(
            state = verticalGridState,
            itemCount = cellCount,
            columns = columns,
            cellHeight = cellHeight,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(bottom = layout.bottomInset),
        )
    }
    if (layout.indicatorVisibility.horizontal) {
        HorizontalScrollIndicator(
            state = horizontalScrollState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(end = layout.endInset),
        )
    }
}

@Composable
private fun RestoreCellFocus(
    index: Int?,
    placedIndex: Int?,
    cellCount: Int,
    gridState: LazyGridState,
    focusRequester: FocusRequester,
    onScrollStarted: () -> Unit,
) {
    LaunchedEffect(index) {
        onScrollStarted()
        val target = index ?: return@LaunchedEffect
        if (target !in 0 until cellCount) return@LaunchedEffect
        val targetIsVisible =
            gridState.layoutInfo.visibleItemsInfo.any { visibleItem ->
                visibleItem.index == target
            }
        if (targetIsVisible) {
            focusRequester.requestFocus()
        } else {
            gridState.scrollToItem(target)
        }
    }
    LaunchedEffect(index, placedIndex) {
        if (index == null || placedIndex != index) return@LaunchedEffect
        focusRequester.requestFocus()
    }
}

@Composable
private fun TableCells(
    columns: Int,
    content: TableLoadState.Content,
    cellHeight: Dp,
    state: LazyGridState,
    restoreFocusIndex: Int?,
    cellFocusRequester: FocusRequester,
    onRestoreTargetPlaced: (Int) -> Unit,
    onIntent: (TableIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cells = content.cells
    val selectedDescription = stringResource(R.string.table_cell_selected)
    val notSelectedDescription = stringResource(R.string.table_cell_not_selected)
    val toggleLabel = stringResource(R.string.table_cell_toggle)
    val editLabel = stringResource(R.string.table_cell_edit)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        modifier =
            modifier
                .semantics {
                    collectionInfo =
                        CollectionInfo(
                            rowCount = (cells.size + columns - 1) / columns,
                            columnCount = columns,
                        )
                },
        contentPadding = PaddingValues(TableContentPadding),
        horizontalArrangement = Arrangement.spacedBy(TableCellSpacing),
        verticalArrangement = Arrangement.spacedBy(TableCellSpacing),
    ) {
        items(
            count = cells.size,
            key = { it },
            contentType = { TableCellContentType },
        ) { index ->
            val cell = cells[index]
            SelectableCell(
                text = cell.text,
                selected = cell.isSelected,
                row = index / columns,
                column = index % columns,
                cellDescription =
                    stringResource(
                        R.string.table_cell_content_description,
                        index / columns + 1,
                        index % columns + 1,
                        cell.text,
                    ),
                selectedDescription = selectedDescription,
                notSelectedDescription = notSelectedDescription,
                toggleLabel = toggleLabel,
                editLabel = editLabel,
                cellHeight = cellHeight,
                onClick = { onIntent(TableIntent.CellClicked(index)) },
                onDoubleClick = { onIntent(TableIntent.CellDoubleClicked(index)) },
                modifier =
                    Modifier.restoreFocusTarget(
                        index = index,
                        restoreFocusIndex = restoreFocusIndex,
                        focusRequester = cellFocusRequester,
                        onPlaced = onRestoreTargetPlaced,
                    ),
            )
        }
    }
}

private fun Modifier.restoreFocusTarget(
    index: Int,
    restoreFocusIndex: Int?,
    focusRequester: FocusRequester,
    onPlaced: (Int) -> Unit,
): Modifier =
    if (index == restoreFocusIndex) {
        focusRequester(focusRequester)
            .onGloballyPositioned { onPlaced(index) }
    } else {
        this
    }

@Composable
private fun VerticalGridScrollIndicator(
    state: LazyGridState,
    itemCount: Int,
    columns: Int,
    cellHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val thumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    Canvas(
        modifier =
            modifier
                .fillMaxHeight()
                .width(ScrollIndicatorThickness)
                .padding(horizontal = 5.dp, vertical = AppSpacing.medium),
    ) {
        val totalRows = (itemCount + columns - 1) / columns
        val cellHeightPx = cellHeight.toPx()
        val spacing = TableCellSpacing.toPx()
        val contentPadding = TableContentPadding.toPx()
        val contentSize =
            contentPadding * 2 + totalRows * cellHeightPx + (totalRows - 1).coerceAtLeast(0) * spacing
        val viewportSize =
            state.layoutInfo.viewportSize.height
                .toFloat()
        val trackHeight = size.height
        val firstVisibleRow = state.firstVisibleItemIndex / columns
        val scrollOffset = firstVisibleRow * (cellHeightPx + spacing) + state.firstVisibleItemScrollOffset
        val geometry =
            calculateScrollThumbGeometry(
                trackLength = trackHeight,
                viewportLength = viewportSize,
                contentLength = contentSize,
                scrollOffset = scrollOffset,
                minimumThumbLength = 32.dp.toPx(),
            ) ?: return@Canvas

        val cornerRadius = CornerRadius(size.width / 2f)
        drawRoundRect(color = trackColor, cornerRadius = cornerRadius)

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(0f, geometry.offset),
            size = Size(size.width, geometry.length),
            cornerRadius = cornerRadius,
        )
    }
}

@Composable
private fun HorizontalScrollIndicator(
    state: ScrollState,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val thumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ScrollIndicatorThickness)
                .padding(horizontal = AppSpacing.medium, vertical = 5.dp),
    ) {
        val viewportSize = state.viewportSize.toFloat()
        val geometry =
            calculateScrollThumbGeometry(
                trackLength = size.width,
                viewportLength = viewportSize,
                contentLength = viewportSize + state.maxValue,
                scrollOffset = state.value.toFloat(),
                minimumThumbLength = 32.dp.toPx(),
            ) ?: return@Canvas

        val cornerRadius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = cornerRadius)
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(geometry.offset, 0f),
            size = Size(geometry.length, size.height),
            cornerRadius = cornerRadius,
        )
    }
}

@Preview(name = "Table grid", widthDp = 700, heightDp = 420)
@Composable
private fun TableGridPreview() {
    val cells =
        List(40) { CellUiState(text = "Cell $it", isSelected = it % 3 == 0) }
            .toPersistentList()
    AppTheme {
        TableGrid(
            columns = 4,
            content = TableLoadState.Content(cells),
            onIntent = {},
        )
    }
}
