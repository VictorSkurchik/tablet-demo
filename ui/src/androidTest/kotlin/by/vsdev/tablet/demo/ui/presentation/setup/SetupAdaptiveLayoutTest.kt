package by.vsdev.tablet.demo.ui.presentation.setup

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class SetupAdaptiveLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun setupChangesFromSingleToDualPaneWhenWindowExpands() {
        var window by mutableStateOf(TestWindow(width = 400, height = 700))
        setSetupScreen { window }

        composeRule.onNodeWithTag(SETUP_FORM_PANE_TAG).assertExists()
        composeRule.onNodeWithTag(SETUP_SUPPORTING_PANE_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(SETUP_PANE_DRAG_HANDLE_TAG).assertDoesNotExist()
        composeRule.onNode(hasSetTextAction() and hasText("Rows")).performTextInput("4")
        composeRule.onNode(hasSetTextAction() and hasText("Columns")).performTextInput("3")
        composeRule.onNode(hasSetTextAction() and hasText("Columns")).assertIsFocused()

        composeRule.runOnIdle { window = window.copy(width = 900) }

        composeRule.onNodeWithTag(SETUP_FORM_PANE_TAG).assertExists()
        composeRule.onNodeWithTag(SETUP_SUPPORTING_PANE_TAG).assertExists()
        composeRule.onNodeWithTag(SETUP_PANE_DRAG_HANDLE_TAG).assertHasClickAction()
        composeRule.onAllNodesWithText("Table size").assertCountEquals(1)
        composeRule.onNode(hasSetTextAction() and hasText("Rows")).assertTextContains("4")
        composeRule
            .onNode(hasSetTextAction() and hasText("Columns"))
            .assertTextContains("3")
            .assertIsFocused()
        composeRule.onNodeWithText("Build table").assertExists()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            val currentIntroBounds =
                composeRule
                    .onNodeWithTag(SETUP_SUPPORTING_PANE_TAG)
                    .fetchSemanticsNode()
                    .boundsInRoot
            val currentFormBounds =
                composeRule
                    .onNodeWithTag(SETUP_FORM_PANE_TAG)
                    .fetchSemanticsNode()
                    .boundsInRoot
            abs(currentIntroBounds.width - currentFormBounds.width) <= 1f
        }
        val introBounds =
            composeRule.onNodeWithTag(SETUP_SUPPORTING_PANE_TAG).fetchSemanticsNode().boundsInRoot
        val formBounds =
            composeRule.onNodeWithTag(SETUP_FORM_PANE_TAG).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "The setup introduction should be before the form in a wide window",
            introBounds.right <= formBounds.left,
        )
        assertTrue(
            "The setup divider should start at the window midpoint: " +
                "intro=$introBounds, form=$formBounds",
            abs(introBounds.width - formBounds.width) <= 1f,
        )

        composeRule
            .onNodeWithTag(SETUP_PANE_DRAG_HANDLE_TAG)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            val resizedIntroWidth =
                composeRule
                    .onNodeWithTag(SETUP_SUPPORTING_PANE_TAG)
                    .fetchSemanticsNode()
                    .boundsInRoot.width
            abs(resizedIntroWidth - introBounds.width) > 1f
        }
    }

    @Test
    fun portraitTabletUsesEqualVerticalSetupPanes() {
        setSetupScreen { TestWindow(width = 800, height = 1_280) }

        val introBounds =
            composeRule.onNodeWithTag(SETUP_SUPPORTING_PANE_TAG).fetchSemanticsNode().boundsInRoot
        val formBounds =
            composeRule.onNodeWithTag(SETUP_FORM_PANE_TAG).fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithTag(SETUP_PANE_DRAG_HANDLE_TAG).assertDoesNotExist()
        assertTrue(
            "The setup introduction should be above the form in portrait",
            introBounds.bottom <= formBounds.top,
        )
        assertTrue(
            "Portrait setup panes should share the height evenly",
            abs(introBounds.height - formBounds.height) <= 1f,
        )
    }

    @Test
    fun offCenterTabletopHingePlacesSetupPanesInItsPhysicalRegions() {
        val hingeBounds =
            with(composeRule.density) {
                Rect(
                    left = 0f,
                    top = 320.dp.toPx(),
                    right = 900.dp.toPx(),
                    bottom = 400.dp.toPx(),
                )
            }
        val window =
            TestWindow(
                width = 900,
                height = 1_200,
                posture =
                    Posture(
                        isTabletop = true,
                        hingeList =
                            listOf(
                                HingeInfo(
                                    bounds = hingeBounds,
                                    isFlat = false,
                                    isVertical = false,
                                    isSeparating = true,
                                    isOccluding = true,
                                ),
                            ),
                    ),
            )
        setSetupScreen { window }

        val formBounds =
            composeRule.onNodeWithTag(SETUP_FORM_PANE_TAG).fetchSemanticsNode().boundsInRoot
        val supportingBounds =
            composeRule.onNodeWithTag(SETUP_SUPPORTING_PANE_TAG).fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithTag(SETUP_PANE_DRAG_HANDLE_TAG).assertDoesNotExist()
        assertTrue(
            "The setup introduction should end at the physical hinge",
            abs(supportingBounds.bottom - hingeBounds.top) <= 1f,
        )
        assertTrue(
            "The setup form should start after the physical hinge",
            abs(formBounds.top - hingeBounds.bottom) <= 1f,
        )
    }

    private fun setSetupScreen(windowProvider: () -> TestWindow) {
        val rowsInput = TextFieldState()
        val columnsInput = TextFieldState()
        composeRule.setContent {
            val window = windowProvider()
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(
                    DpSize(width = window.width.dp, height = window.height.dp),
                ),
            ) {
                AppTheme {
                    SetupScreen(
                        state = SetupUiState(),
                        rowsInput = rowsInput,
                        columnsInput = columnsInput,
                        onBuild = {},
                        windowAdaptiveInfo = window.toAdaptiveInfo(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun TestWindow.toAdaptiveInfo(): WindowAdaptiveInfo =
        WindowAdaptiveInfo(
            windowSizeClass =
                WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(
                    widthDp = width,
                    heightDp = height,
                ),
            windowPosture = posture,
        )

    private data class TestWindow(
        val width: Int,
        val height: Int,
        val posture: Posture = Posture(),
    )
}
