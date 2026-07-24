package by.vsdev.tablet.demo.ui.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * Places two panes in adjacent physical regions separated by horizontal hinges.
 *
 * Material 3 Adaptive currently reflows panes around a centered horizontal partition. Window
 * Manager can report off-center or multiple hinges, so using all real bounds is necessary to keep
 * visible and interactive content out of every separating area.
 */
@Composable
internal fun HorizontalHingePaneScaffold(
    hingeBounds: List<Rect>,
    excludedBounds: List<Rect>,
    mainPane: @Composable () -> Unit,
    supportingPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    mainPaneModifier: Modifier = Modifier,
    supportingPaneModifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        content = {
            Box(Modifier.fillMaxSize().then(mainPaneModifier)) { mainPane() }
            Box(Modifier.fillMaxSize().then(supportingPaneModifier)) { supportingPane() }
        },
    ) { measurables, constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val paneRegions =
            calculateSafePaneRegions(
                layoutWidth = layoutWidth,
                layoutHeight = layoutHeight,
                horizontalHingeBounds = hingeBounds,
                excludedBounds = excludedBounds,
            )

        val mainPanePlaceable =
            measurables[0].measure(
                Constraints.fixed(
                    width = paneRegions.main.width,
                    height = paneRegions.main.height,
                ),
            )
        val supportingPanePlaceable =
            measurables[1].measure(
                Constraints.fixed(
                    width = paneRegions.supporting.width,
                    height = paneRegions.supporting.height,
                ),
            )

        layout(layoutWidth, layoutHeight) {
            mainPanePlaceable.place(x = paneRegions.main.left, y = paneRegions.main.top)
            supportingPanePlaceable.place(
                x = paneRegions.supporting.left,
                y = paneRegions.supporting.top,
            )
        }
    }
}

internal data class SafePaneRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int = right - left
    val height: Int = bottom - top
}

internal data class SafePaneRegions(
    val main: SafePaneRegion,
    val supporting: SafePaneRegion,
)

internal data class VerticalPaneRegion(
    val top: Int,
    val bottom: Int,
) {
    val height: Int = bottom - top
}

internal data class HorizontalPaneRegions(
    val main: VerticalPaneRegion,
    val supporting: VerticalPaneRegion,
)

internal fun calculateSafePaneRegions(
    layoutWidth: Int,
    layoutHeight: Int,
    horizontalHingeBounds: List<Rect>,
    excludedBounds: List<Rect>,
): SafePaneRegions {
    val verticalPaneRegions =
        calculateHorizontalPaneRegions(
            layoutHeight = layoutHeight,
            hingeBounds = horizontalHingeBounds,
        )
    return SafePaneRegions(
        main =
            verticalPaneRegions.main.toSafePaneRegion(
                layoutWidth = layoutWidth,
                excludedBounds = excludedBounds,
            ),
        supporting =
            verticalPaneRegions.supporting.toSafePaneRegion(
                layoutWidth = layoutWidth,
                excludedBounds = excludedBounds,
            ),
    )
}

internal fun calculateHorizontalPaneRegions(
    layoutHeight: Int,
    hingeBounds: List<Rect>,
): HorizontalPaneRegions {
    val occupiedRegions = mergeOccupiedRegions(layoutHeight, hingeBounds)
    val safeRegions = buildSafeRegions(layoutHeight, occupiedRegions)
    val selectedRegionIndex = selectBestAdjacentRegionPair(safeRegions)
    return HorizontalPaneRegions(
        main = safeRegions[selectedRegionIndex],
        supporting = safeRegions[selectedRegionIndex + 1],
    )
}

private fun mergeOccupiedRegions(
    layoutHeight: Int,
    hingeBounds: List<Rect>,
): List<VerticalPaneRegion> {
    val sortedRegions =
        hingeBounds
            .map { bounds ->
                val top = bounds.top.roundToInt().coerceIn(0, layoutHeight)
                val bottom = bounds.bottom.roundToInt().coerceIn(top, layoutHeight)
                VerticalPaneRegion(top, bottom)
            }.sortedBy(VerticalPaneRegion::top)
    if (sortedRegions.isEmpty()) {
        val center = layoutHeight / 2
        return listOf(VerticalPaneRegion(center, center))
    }

    return buildList {
        sortedRegions.forEach { region ->
            val previous = lastOrNull()
            if (previous == null || region.top > previous.bottom) {
                add(region)
            } else if (region.bottom > previous.bottom) {
                removeAt(lastIndex)
                add(previous.copy(bottom = region.bottom))
            }
        }
    }
}

private fun buildSafeRegions(
    layoutHeight: Int,
    occupiedRegions: List<VerticalPaneRegion>,
): List<VerticalPaneRegion> =
    buildList {
        var regionTop = 0
        occupiedRegions.forEach { occupiedRegion ->
            add(VerticalPaneRegion(regionTop, occupiedRegion.top))
            regionTop = occupiedRegion.bottom
        }
        add(VerticalPaneRegion(regionTop, layoutHeight))
    }

private fun selectBestAdjacentRegionPair(safeRegions: List<VerticalPaneRegion>): Int {
    var bestIndex = 0
    var bestCombinedHeight = -1
    var bestMinimumHeight = -1
    safeRegions.zipWithNext().forEachIndexed { index, (first, second) ->
        val combinedHeight = first.height + second.height
        val minimumHeight = minOf(first.height, second.height)
        if (
            combinedHeight > bestCombinedHeight ||
            (combinedHeight == bestCombinedHeight && minimumHeight > bestMinimumHeight)
        ) {
            bestIndex = index
            bestCombinedHeight = combinedHeight
            bestMinimumHeight = minimumHeight
        }
    }
    return bestIndex
}

private fun VerticalPaneRegion.toSafePaneRegion(
    layoutWidth: Int,
    excludedBounds: List<Rect>,
): SafePaneRegion {
    val occupiedHorizontalRegions =
        excludedBounds
            .filter { bounds -> bounds.top < bottom && bounds.bottom > top }
            .map { bounds ->
                val left = bounds.left.roundToInt().coerceIn(0, layoutWidth)
                val right = bounds.right.roundToInt().coerceIn(left, layoutWidth)
                HorizontalPaneRegion(left, right)
            }.sortedBy(HorizontalPaneRegion::left)
            .mergeOverlapping()
    val widestSafeRegion = findWidestSafeHorizontalRegion(layoutWidth, occupiedHorizontalRegions)
    return SafePaneRegion(
        left = widestSafeRegion.left,
        top = top,
        right = widestSafeRegion.right,
        bottom = bottom,
    )
}

private data class HorizontalPaneRegion(
    val left: Int,
    val right: Int,
) {
    val width: Int = right - left
}

private fun List<HorizontalPaneRegion>.mergeOverlapping(): List<HorizontalPaneRegion> =
    buildList {
        this@mergeOverlapping.forEach { region ->
            val previous = lastOrNull()
            if (previous == null || region.left > previous.right) {
                add(region)
            } else if (region.right > previous.right) {
                removeAt(lastIndex)
                add(previous.copy(right = region.right))
            }
        }
    }

private fun findWidestSafeHorizontalRegion(
    layoutWidth: Int,
    occupiedRegions: List<HorizontalPaneRegion>,
): HorizontalPaneRegion {
    var safeRegionStart = 0
    var widestSafeRegion = HorizontalPaneRegion(left = 0, right = 0)
    occupiedRegions.forEach { occupiedRegion ->
        val safeRegion = HorizontalPaneRegion(safeRegionStart, occupiedRegion.left)
        if (safeRegion.width > widestSafeRegion.width) {
            widestSafeRegion = safeRegion
        }
        safeRegionStart = occupiedRegion.right
    }
    val trailingSafeRegion = HorizontalPaneRegion(safeRegionStart, layoutWidth)
    return if (trailingSafeRegion.width > widestSafeRegion.width) {
        trailingSafeRegion
    } else {
        widestSafeRegion
    }
}
