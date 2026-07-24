package by.vsdev.tablet.demo.ui.components.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class AdaptivePanePolicyTest {
    @Test
    fun compactWindowUsesSingleHorizontalPane() {
        val directive =
            calculateAppPaneScaffoldDirective(
                adaptiveInfo(width = 400, height = 700),
                density = Density(1f),
            )

        assertEquals(1, directive.maxHorizontalPartitions)
    }

    @Test
    fun expandedWindowUsesTwoHorizontalPanes() {
        val directive =
            calculateAppPaneScaffoldDirective(
                adaptiveInfo(width = 900, height = 700),
                density = Density(1f),
            )

        assertEquals(2, directive.maxHorizontalPartitions)
    }

    @Test
    fun tabletopPostureUsesOnlyTwoVerticalPanes() {
        val directive =
            calculateAppPaneScaffoldDirective(
                adaptiveInfo(
                    width = 900,
                    height = 800,
                    posture = Posture(isTabletop = true),
                ),
                density = Density(1f),
            )

        assertEquals(1, directive.maxHorizontalPartitions)
        assertEquals(2, directive.maxVerticalPartitions)
        assertEquals(0.dp, directive.horizontalPartitionSpacerSize)
    }

    @Test
    fun flatSeparatingHorizontalHingeUsesVerticalPanesAndCoversItsBounds() {
        val posture =
            Posture(
                isTabletop = false,
                hingeList =
                    listOf(
                        hinge(
                            bounds = Rect(left = 0f, top = 360f, right = 900f, bottom = 440f),
                            isSeparating = true,
                            isVertical = false,
                        ),
                    ),
            )

        val directive =
            calculateAppPaneScaffoldDirective(
                adaptiveInfo(width = 900, height = 800, posture = posture),
                density = Density(2f),
            )

        assertEquals(1, directive.maxHorizontalPartitions)
        assertEquals(2, directive.maxVerticalPartitions)
        assertEquals(40.dp, directive.verticalPartitionSpacerSize)
        assertEquals(emptyList<Rect>(), directive.excludedBounds)
    }

    @Test
    fun multipleHorizontalHingesSelectLargestAdjacentSafeRegions() {
        val paneRegions =
            calculateHorizontalPaneRegions(
                layoutHeight = 1_200,
                hingeBounds =
                    listOf(
                        Rect(left = 0f, top = 300f, right = 900f, bottom = 340f),
                        Rect(left = 0f, top = 700f, right = 900f, bottom = 760f),
                    ),
            )

        assertEquals(VerticalPaneRegion(top = 340, bottom = 700), paneRegions.main)
        assertEquals(VerticalPaneRegion(top = 760, bottom = 1_200), paneRegions.supporting)
    }

    @Test
    fun mixedOrientationHingesKeepBothPanesInsideWidestTwoDimensionalSafeRegions() {
        val paneRegions =
            calculateSafePaneRegions(
                layoutWidth = 1_000,
                layoutHeight = 1_200,
                horizontalHingeBounds =
                    listOf(
                        Rect(left = 0f, top = 400f, right = 1_000f, bottom = 460f),
                    ),
                excludedBounds =
                    listOf(
                        Rect(left = 300f, top = 0f, right = 340f, bottom = 1_200f),
                    ),
            )

        assertEquals(
            SafePaneRegion(left = 340, top = 0, right = 1_000, bottom = 400),
            paneRegions.main,
        )
        assertEquals(
            SafePaneRegion(left = 340, top = 460, right = 1_000, bottom = 1_200),
            paneRegions.supporting,
        )
    }

    @Test
    fun onlySeparatingVerticalHingesAreExcluded() {
        val separatingHinge = Rect(left = 596f, top = 0f, right = 604f, bottom = 800f)
        val flatHinge = Rect(left = 300f, top = 0f, right = 302f, bottom = 800f)
        val posture =
            Posture(
                hingeList =
                    listOf(
                        hinge(bounds = separatingHinge, isSeparating = true),
                        hinge(bounds = flatHinge, isSeparating = false),
                    ),
            )

        val directive =
            calculateAppPaneScaffoldDirective(
                adaptiveInfo(width = 1_200, height = 800, posture = posture),
                density = Density(1f),
            )

        assertEquals(listOf(separatingHinge), directive.excludedBounds)
    }

    private fun adaptiveInfo(
        width: Int,
        height: Int,
        posture: Posture = Posture(),
    ): WindowAdaptiveInfo =
        WindowAdaptiveInfo(
            windowSizeClass =
                WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(
                    widthDp = width,
                    heightDp = height,
                ),
            windowPosture = posture,
        )

    private fun hinge(
        bounds: Rect,
        isSeparating: Boolean,
        isVertical: Boolean = true,
    ): HingeInfo =
        HingeInfo(
            bounds = bounds,
            isFlat = !isSeparating,
            isVertical = isVertical,
            isSeparating = isSeparating,
            isOccluding = false,
        )
}
