package by.vsdev.tablet.demo.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import by.vsdev.tablet.demo.ui.components.molecules.SelectableCell
import by.vsdev.tablet.demo.ui.haptics.AppHaptics
import by.vsdev.tablet.demo.ui.haptics.LocalAppHaptics
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SelectableCellTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedCell_exposesStateAndClickAction() {
        var clicks = 0
        val haptics = RecordingAppHaptics()
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalAppHaptics provides haptics) {
                    SelectableCell(
                        text = "Cell value",
                        selected = true,
                        row = 0,
                        column = 1,
                        cellDescription = "Row 1, column 2: Cell value",
                        selectedDescription = "Selected",
                        notSelectedDescription = "Not selected",
                        toggleLabel = "Toggle cell color",
                        editLabel = "Edit cell",
                        onClick = { clicks++ },
                        onDoubleClick = {},
                    )
                }
            }
        }

        composeRule
            .onNodeWithContentDescription("Row 1, column 2: Cell value")
            .assertIsSelected()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Selected",
                ),
            ).performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) { clicks == 1 }
        assertEquals(1, clicks)
        assertEquals(listOf(false), haptics.selectionStates)
    }

    @Test
    fun doubleClick_invokesEditWithoutTogglingSelection() {
        var clicks = 0
        var edits = 0
        val haptics = RecordingAppHaptics()
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalAppHaptics provides haptics) {
                    SelectableCell(
                        text = "Cell value",
                        selected = false,
                        row = 0,
                        column = 1,
                        cellDescription = "Row 1, column 2: Cell value",
                        selectedDescription = "Selected",
                        notSelectedDescription = "Not selected",
                        toggleLabel = "Toggle cell color",
                        editLabel = "Edit cell",
                        onClick = { clicks++ },
                        onDoubleClick = { edits++ },
                    )
                }
            }
        }

        composeRule
            .onNodeWithContentDescription("Row 1, column 2: Cell value")
            .performTouchInput { doubleClick() }

        composeRule.runOnIdle {
            assertEquals(0, clicks)
            assertEquals(1, edits)
            assertEquals(emptyList<Boolean>(), haptics.selectionStates)
            assertEquals(1, haptics.editCount)
        }
    }

    @Test
    fun largeFontScale_increasesCellHeightToAvoidClipping() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 3f)) {
                AppTheme {
                    SelectableCell(
                        text = "Large text",
                        selected = false,
                        row = 0,
                        column = 0,
                        cellDescription = "Large text cell",
                        selectedDescription = "Selected",
                        notSelectedDescription = "Not selected",
                        toggleLabel = "Toggle cell color",
                        editLabel = "Edit cell",
                        onClick = {},
                        onDoubleClick = {},
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Large text cell").assertHeightIsAtLeast(76.dp)
    }

    @Test
    fun cellExposesNamedPrimaryAndGestureAlternativeActions() {
        var selections = 0
        var edits = 0
        setCell(
            onClick = { selections++ },
            onDoubleClick = { edits++ },
        )
        val cell = composeRule.onNodeWithContentDescription(CELL_DESCRIPTION)

        val primaryActionLabel = cell.fetchSemanticsNode().config[SemanticsActions.OnClick].label
        assertEquals("Toggle selection", primaryActionLabel)
        cell.performCustomAccessibilityActionWithLabel("Edit cell")

        composeRule.runOnIdle {
            assertEquals(0, selections)
            assertEquals(1, edits)
        }
    }

    @Test
    fun enterTogglesSelectionWhileF2OpensEditor() {
        var selections = 0
        var edits = 0
        val focusHandle =
            setCell(
                onClick = { selections++ },
                onDoubleClick = { edits++ },
            )
        val cell = composeRule.onNodeWithContentDescription(CELL_DESCRIPTION)

        composeRule.runOnIdle {
            focusHandle.inputModeManager.requestInputMode(InputMode.Keyboard)
            focusHandle.focusRequester.requestFocus()
        }
        cell.assertIsFocused()
        cell.performKeyInput { pressKey(Key.Enter) }
        cell.performKeyInput { pressKey(Key.F2) }

        composeRule.runOnIdle {
            assertEquals(1, selections)
            assertEquals(1, edits)
        }
    }

    private fun setCell(
        onClick: () -> Unit,
        onDoubleClick: () -> Unit,
    ): CellFocusHandle {
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        composeRule.setContent {
            inputModeManager = LocalInputModeManager.current
            AppTheme {
                SelectableCell(
                    text = "Cell value",
                    selected = false,
                    row = 0,
                    column = 1,
                    cellDescription = CELL_DESCRIPTION,
                    selectedDescription = "Selected",
                    notSelectedDescription = "Not selected",
                    toggleLabel = "Toggle selection",
                    editLabel = "Edit cell",
                    onClick = onClick,
                    onDoubleClick = onDoubleClick,
                    modifier = Modifier.focusRequester(focusRequester),
                )
            }
        }
        return CellFocusHandle(focusRequester, inputModeManager)
    }

    private data class CellFocusHandle(
        val focusRequester: FocusRequester,
        val inputModeManager: InputModeManager,
    )

    private class RecordingAppHaptics : AppHaptics {
        val selectionStates = mutableListOf<Boolean>()
        var editCount = 0

        override fun performCellSelection(isSelected: Boolean) {
            selectionStates += isSelected
        }

        override fun performCellEdit() {
            editCount++
        }
    }

    private companion object {
        const val CELL_DESCRIPTION = "Row 1, column 2: Cell value"
    }
}
