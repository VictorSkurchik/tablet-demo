package by.vsdev.tablet.demo

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.device.EspressoDevice.Companion.onDevice
import androidx.test.espresso.device.action.ScreenOrientation
import androidx.test.espresso.device.action.setDisplaySize
import androidx.test.espresso.device.filter.RequiresDisplay
import androidx.test.espresso.device.rules.DisplaySizeRule
import androidx.test.espresso.device.rules.ScreenOrientationRule
import androidx.test.espresso.device.sizeclass.HeightSizeClass
import androidx.test.espresso.device.sizeclass.HeightSizeClass.Companion.HeightSizeClassEnum
import androidx.test.espresso.device.sizeclass.WidthSizeClass
import androidx.test.espresso.device.sizeclass.WidthSizeClass.Companion.WidthSizeClassEnum
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.layout.FoldingFeature.Orientation.Companion.HORIZONTAL
import androidx.window.layout.FoldingFeature.Orientation.Companion.VERTICAL
import androidx.window.layout.FoldingFeature.State.Companion.FLAT
import androidx.window.layout.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@RequiresDisplay(
    widthSizeClass = WidthSizeClassEnum.EXPANDED,
    heightSizeClass = HeightSizeClassEnum.MEDIUM,
)
class AdaptiveWindowIntegrationTest {
    @get:Rule(order = 0)
    val windowLayoutInfoRule = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val screenOrientationRule = ScreenOrientationRule(ScreenOrientation.LANDSCAPE)

    @get:Rule(order = 3)
    val displaySizeRule = DisplaySizeRule()

    @Test
    fun verticalHingeKeepsSetupPanesOnOppositeSides() {
        setDisplaySize(WidthSizeClass.EXPANDED)
        waitForSetupPanes()
        val hinge =
            FoldingFeature(
                activity = composeRule.activity,
                center =
                    composeRule.activity.window.decorView.width /
                        VERTICAL_HINGE_CENTER_DIVISOR,
                size = VERTICAL_HINGE_SIZE_PX,
                state = HALF_OPENED,
                orientation = VERTICAL,
            )

        windowLayoutInfoRule.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(hinge)))

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            setupPanesAreOnOppositeSidesOf(
                hingeLeft = hinge.bounds.left.toFloat(),
                hingeRight = hinge.bounds.right.toFloat(),
            )
        }

        assertTrue(
            "Setup panes should not overlap the separating vertical hinge",
            setupPanesAreOnOppositeSidesOf(
                hingeLeft = hinge.bounds.left.toFloat(),
                hingeRight = hinge.bounds.right.toFloat(),
            ),
        )
    }

    @Test
    fun flatHorizontalHingeKeepsSetupPanesAboveAndBelowIt() {
        setDisplaySize(WidthSizeClass.EXPANDED)
        waitForSetupPanes()
        val hinge =
            FoldingFeature(
                activity = composeRule.activity,
                center =
                    composeRule.activity.window.decorView.height /
                        HORIZONTAL_HINGE_CENTER_DIVISOR,
                size = HORIZONTAL_HINGE_SIZE_PX,
                state = FLAT,
                orientation = HORIZONTAL,
            )

        windowLayoutInfoRule.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(hinge)))

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            setupPanesAreAboveAndBelow(
                hingeTop = hinge.bounds.top.toFloat(),
                hingeBottom = hinge.bounds.bottom.toFloat(),
            )
        }

        assertTrue(
            "Setup panes should not overlap the separating horizontal hinge",
            setupPanesAreAboveAndBelow(
                hingeTop = hinge.bounds.top.toFloat(),
                hingeBottom = hinge.bounds.bottom.toFloat(),
            ),
        )
    }

    @Test
    fun multipleHorizontalHingesKeepSetupPanesInAdjacentSafeRegions() {
        setDisplaySize(WidthSizeClass.EXPANDED)
        waitForSetupPanes()
        val windowHeight = composeRule.activity.window.decorView.height
        val firstHinge =
            FoldingFeature(
                activity = composeRule.activity,
                center = windowHeight / FIRST_HORIZONTAL_HINGE_CENTER_DIVISOR,
                size = HORIZONTAL_HINGE_SIZE_PX,
                state = FLAT,
                orientation = HORIZONTAL,
            )
        val secondHinge =
            FoldingFeature(
                activity = composeRule.activity,
                center =
                    windowHeight *
                        SECOND_HORIZONTAL_HINGE_CENTER_NUMERATOR /
                        SECOND_HORIZONTAL_HINGE_CENTER_DENOMINATOR,
                size = HORIZONTAL_HINGE_SIZE_PX,
                state = FLAT,
                orientation = HORIZONTAL,
            )

        windowLayoutInfoRule.overrideWindowLayoutInfo(
            TestWindowLayoutInfo(listOf(firstHinge, secondHinge)),
        )

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            setupPanesOccupyRegionsBetweenAndBelow(
                firstHingeBottom = firstHinge.bounds.bottom.toFloat(),
                secondHingeTop = secondHinge.bounds.top.toFloat(),
                secondHingeBottom = secondHinge.bounds.bottom.toFloat(),
                windowBottom = windowHeight.toFloat(),
            )
        }

        assertTrue(
            "Setup panes should occupy two adjacent regions without crossing either hinge",
            setupPanesOccupyRegionsBetweenAndBelow(
                firstHingeBottom = firstHinge.bounds.bottom.toFloat(),
                secondHingeTop = secondHinge.bounds.top.toFloat(),
                secondHingeBottom = secondHinge.bounds.bottom.toFloat(),
                windowBottom = windowHeight.toFloat(),
            ),
        )
    }

    @Test
    fun mixedOrientationHingesKeepSetupPanesInsideTwoDimensionalSafeRegions() {
        setDisplaySize(WidthSizeClass.EXPANDED)
        waitForSetupPanes()
        val windowWidth = composeRule.activity.window.decorView.width
        val windowHeight = composeRule.activity.window.decorView.height
        val horizontalHinge =
            FoldingFeature(
                activity = composeRule.activity,
                center = windowHeight / HORIZONTAL_HINGE_CENTER_DIVISOR,
                size = HORIZONTAL_HINGE_SIZE_PX,
                state = FLAT,
                orientation = HORIZONTAL,
            )
        val verticalHinge =
            FoldingFeature(
                activity = composeRule.activity,
                center = windowWidth / VERTICAL_HINGE_CENTER_DIVISOR,
                size = VERTICAL_HINGE_SIZE_PX,
                state = HALF_OPENED,
                orientation = VERTICAL,
            )

        windowLayoutInfoRule.overrideWindowLayoutInfo(
            TestWindowLayoutInfo(listOf(horizontalHinge, verticalHinge)),
        )

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            setupPanesOccupyMixedHingeSafeRegions(
                verticalHingeRight = verticalHinge.bounds.right.toFloat(),
                horizontalHingeTop = horizontalHinge.bounds.top.toFloat(),
                horizontalHingeBottom = horizontalHinge.bounds.bottom.toFloat(),
                windowRight = windowWidth.toFloat(),
                windowBottom = windowHeight.toFloat(),
            )
        }

        assertTrue(
            "Setup panes should avoid both separating hinges",
            setupPanesOccupyMixedHingeSafeRegions(
                verticalHingeRight = verticalHinge.bounds.right.toFloat(),
                horizontalHingeTop = horizontalHinge.bounds.top.toFloat(),
                horizontalHingeBottom = horizontalHinge.bounds.bottom.toFloat(),
                windowRight = windowWidth.toFloat(),
                windowBottom = windowHeight.toFloat(),
            ),
        )
    }

    @Test
    fun expandedCompactExpandedResizePreservesEditorDraft() {
        setDisplaySize(WidthSizeClass.EXPANDED)
        buildTable()
        composeRule
            .onNodeWithContentDescription("Row 1, column 1:", substring = true)
            .performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(EDITOR_FIELD_TAG).performTextReplacement(DRAFT)

        setDisplaySize(WidthSizeClass.COMPACT)
        waitForTablePanes(mainVisible = false, editorVisible = true)
        composeRule.onNodeWithTag(EDITOR_FIELD_TAG).assertTextContains(DRAFT)

        setDisplaySize(WidthSizeClass.EXPANDED)
        waitForTablePanes(mainVisible = true, editorVisible = true)
        composeRule.onNodeWithTag(EDITOR_FIELD_TAG).assertTextContains(DRAFT)
    }

    private fun setDisplaySize(widthSizeClass: WidthSizeClass) {
        onDevice()
            .perform(
                setDisplaySize(
                    widthSizeClass = widthSizeClass,
                    heightSizeClass = HeightSizeClass.MEDIUM,
                ),
            )
        composeRule.waitForIdle()
    }

    private fun buildTable() {
        composeRule.onNode(hasSetTextAction() and hasText("Rows")).performTextInput("2")
        composeRule.onNode(hasSetTextAction() and hasText("Columns")).performTextInput("2")
        composeRule.onNodeWithText("Build table").performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText("Table · 2 × 2").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTablePanes(
        mainVisible: Boolean,
        editorVisible: Boolean,
    ) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithTag(TABLE_MAIN_PANE_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty() == mainVisible &&
                composeRule
                    .onAllNodesWithTag(TABLE_EDITOR_PANE_TAG)
                    .fetchSemanticsNodes()
                    .isNotEmpty() == editorVisible
        }
    }

    private fun waitForSetupPanes() {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithTag(SETUP_FORM_PANE_TAG)
                .fetchSemanticsNodes()
                .size == 1 &&
                composeRule
                    .onAllNodesWithTag(SETUP_SUPPORTING_PANE_TAG)
                    .fetchSemanticsNodes()
                    .size == 1
        }
    }

    private fun setupPanesAreOnOppositeSidesOf(
        hingeLeft: Float,
        hingeRight: Float,
    ): Boolean {
        val formNodes =
            composeRule.onAllNodesWithTag(SETUP_FORM_PANE_TAG).fetchSemanticsNodes()
        val supportingNodes =
            composeRule.onAllNodesWithTag(SETUP_SUPPORTING_PANE_TAG).fetchSemanticsNodes()
        if (formNodes.size != 1 || supportingNodes.size != 1) return false

        val formBounds = formNodes.single().boundsInRoot
        val supportingBounds = supportingNodes.single().boundsInRoot
        return (formBounds.right <= hingeLeft && supportingBounds.left >= hingeRight) ||
            (supportingBounds.right <= hingeLeft && formBounds.left >= hingeRight)
    }

    private fun setupPanesAreAboveAndBelow(
        hingeTop: Float,
        hingeBottom: Float,
    ): Boolean {
        val formNodes =
            composeRule.onAllNodesWithTag(SETUP_FORM_PANE_TAG).fetchSemanticsNodes()
        val supportingNodes =
            composeRule.onAllNodesWithTag(SETUP_SUPPORTING_PANE_TAG).fetchSemanticsNodes()
        if (formNodes.size != 1 || supportingNodes.size != 1) return false

        val formBounds = formNodes.single().boundsInRoot
        val supportingBounds = supportingNodes.single().boundsInRoot
        return (formBounds.bottom <= hingeTop && supportingBounds.top >= hingeBottom) ||
            (supportingBounds.bottom <= hingeTop && formBounds.top >= hingeBottom)
    }

    private fun setupPanesOccupyRegionsBetweenAndBelow(
        firstHingeBottom: Float,
        secondHingeTop: Float,
        secondHingeBottom: Float,
        windowBottom: Float,
    ): Boolean {
        val formNodes =
            composeRule.onAllNodesWithTag(SETUP_FORM_PANE_TAG).fetchSemanticsNodes()
        val supportingNodes =
            composeRule.onAllNodesWithTag(SETUP_SUPPORTING_PANE_TAG).fetchSemanticsNodes()
        if (formNodes.size != 1 || supportingNodes.size != 1) return false

        val formBounds = formNodes.single().boundsInRoot
        val supportingBounds = supportingNodes.single().boundsInRoot
        return formBounds.top.isWithinOnePixelOf(firstHingeBottom) &&
            formBounds.bottom.isWithinOnePixelOf(secondHingeTop) &&
            supportingBounds.top.isWithinOnePixelOf(secondHingeBottom) &&
            supportingBounds.bottom.isWithinOnePixelOf(windowBottom)
    }

    private fun setupPanesOccupyMixedHingeSafeRegions(
        verticalHingeRight: Float,
        horizontalHingeTop: Float,
        horizontalHingeBottom: Float,
        windowRight: Float,
        windowBottom: Float,
    ): Boolean {
        val formNodes =
            composeRule.onAllNodesWithTag(SETUP_FORM_PANE_TAG).fetchSemanticsNodes()
        val supportingNodes =
            composeRule.onAllNodesWithTag(SETUP_SUPPORTING_PANE_TAG).fetchSemanticsNodes()
        if (formNodes.size != 1 || supportingNodes.size != 1) return false

        val formBounds = formNodes.single().boundsInRoot
        val supportingBounds = supportingNodes.single().boundsInRoot
        return formBounds.left.isWithinOnePixelOf(verticalHingeRight) &&
            formBounds.top.isWithinOnePixelOf(0f) &&
            formBounds.right.isWithinOnePixelOf(windowRight) &&
            formBounds.bottom.isWithinOnePixelOf(horizontalHingeTop) &&
            supportingBounds.left.isWithinOnePixelOf(verticalHingeRight) &&
            supportingBounds.top.isWithinOnePixelOf(horizontalHingeBottom) &&
            supportingBounds.right.isWithinOnePixelOf(windowRight) &&
            supportingBounds.bottom.isWithinOnePixelOf(windowBottom)
    }

    private fun Float.isWithinOnePixelOf(expected: Float): Boolean = abs(this - expected) <= 1f

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val VERTICAL_HINGE_SIZE_PX = 16
        const val VERTICAL_HINGE_CENTER_DIVISOR = 3
        const val HORIZONTAL_HINGE_SIZE_PX = 80
        const val HORIZONTAL_HINGE_CENTER_DIVISOR = 3
        const val FIRST_HORIZONTAL_HINGE_CENTER_DIVISOR = 4
        const val SECOND_HORIZONTAL_HINGE_CENTER_NUMERATOR = 9
        const val SECOND_HORIZONTAL_HINGE_CENTER_DENOMINATOR = 16
        const val DRAFT = "Draft survives real resize"
        const val SETUP_FORM_PANE_TAG = "setupFormPane"
        const val SETUP_SUPPORTING_PANE_TAG = "setupSupportingPane"
        const val TABLE_MAIN_PANE_TAG = "tableMainPane"
        const val TABLE_EDITOR_PANE_TAG = "tableEditorPane"
        const val EDITOR_FIELD_TAG = "editorField"
    }
}
