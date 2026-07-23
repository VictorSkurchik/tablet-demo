package by.vsdev.tablet.demo.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
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
        composeRule.waitForIdle()

        composeRule
            .onNodeWithContentDescription("Row 1, column 2: Cell value")
            .assertIsSelected()
            .performClick()
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
        composeRule.waitForIdle()

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
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Large text cell").assertHeightIsAtLeast(76.dp)
    }

    @Test
    fun enterTogglesSelectionWhileF2OpensEditor() {
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
                        column = 0,
                        cellDescription = "Keyboard cell",
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
        composeRule.waitForIdle()
        val cell = composeRule.onNodeWithContentDescription("Keyboard cell")
        cell.performSemanticsAction(SemanticsActions.RequestFocus)

        cell.performKeyInput { pressKey(Key.Enter) }
        cell.performKeyInput { pressKey(Key.F2) }

        composeRule.runOnIdle {
            assertEquals(1, clicks)
            assertEquals(1, edits)
            assertEquals(listOf(true), haptics.selectionStates)
            assertEquals(1, haptics.editCount)
        }
    }

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
}
