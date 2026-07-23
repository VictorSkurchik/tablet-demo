package by.vsdev.tablet.demo.ui.components.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.separatingHorizontalHingeBounds
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

internal const val EQUAL_VERTICAL_PANE_HEIGHT_FRACTION = 0.5f

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val WindowAdaptiveInfo.horizontalSeparatingHingeBounds
    get() = windowPosture.separatingHorizontalHingeBounds

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val WindowAdaptiveInfo.usesVerticalPanePartitions: Boolean
    get() =
        windowPosture.isTabletop ||
            horizontalSeparatingHingeBounds.isNotEmpty()

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun calculateAppPaneScaffoldDirective(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    density: Density,
): PaneScaffoldDirective {
    val directive =
        calculatePaneScaffoldDirective(
            windowAdaptiveInfo = windowAdaptiveInfo,
            verticalHingePolicy = HingePolicy.AvoidSeparating,
        )
    return if (windowAdaptiveInfo.usesVerticalPanePartitions) {
        val separatingHingeHeight =
            windowAdaptiveInfo.windowPosture.separatingHorizontalHingeBounds
                .maxOfOrNull { bounds -> with(density) { bounds.height.toDp() } }
                ?: 0.dp
        directive.copy(
            maxHorizontalPartitions = 1,
            horizontalPartitionSpacerSize = 0.dp,
            maxVerticalPartitions = 2,
            verticalPartitionSpacerSize =
                maxOf(directive.verticalPartitionSpacerSize, separatingHingeHeight),
        )
    } else {
        directive
    }
}
