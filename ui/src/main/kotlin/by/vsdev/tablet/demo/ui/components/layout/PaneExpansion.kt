package by.vsdev.tablet.demo.ui.components.layout

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

private const val NARROW_PANE_PROPORTION = 0.33f
private const val WIDE_PANE_PROPORTION = 0.67f

internal const val BALANCED_PANE_PROPORTION = 0.5f
internal const val BALANCED_PANE_ANCHOR_INDEX = 1

internal val StandardPaneExpansionAnchors =
    listOf(
        PaneExpansionAnchor.Proportion(NARROW_PANE_PROPORTION),
        PaneExpansionAnchor.Proportion(BALANCED_PANE_PROPORTION),
        PaneExpansionAnchor.Proportion(WIDE_PANE_PROPORTION),
    )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val ThreePaneScaffoldNavigator<*>.supportsHorizontalPaneExpansion: Boolean
    get() =
        scaffoldDirective.maxHorizontalPartitions > 1 &&
            scaffoldDirective.excludedBounds.isEmpty()

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ThreePaneScaffoldScope.AppPaneDragHandle(
    state: PaneExpansionState,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    VerticalDragHandle(
        modifier =
            modifier.paneExpansionDraggable(
                state,
                LocalMinimumInteractiveComponentSize.current,
                interactionSource,
            ),
        interactionSource = interactionSource,
    )
}
