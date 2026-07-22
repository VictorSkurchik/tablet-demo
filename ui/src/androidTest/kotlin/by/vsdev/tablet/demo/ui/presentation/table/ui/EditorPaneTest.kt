package by.vsdev.tablet.demo.ui.presentation.table.ui

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import by.vsdev.tablet.demo.ui.presentation.table.MAX_CELL_TEXT_LENGTH
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditorPaneTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inputDoesNotExceedMaximumCellTextLength() {
        composeRule.setContent {
            AppTheme {
                EditorPane(
                    index = 0,
                    currentText = "",
                    onConfirm = { _, _ -> },
                    onDismiss = {},
                )
            }
        }
        val field = composeRule.onNodeWithTag(EDITOR_FIELD_TAG)
        val maximumText = "x".repeat(MAX_CELL_TEXT_LENGTH)

        field.performTextInput(maximumText)
        field.assertTextContains(maximumText)
        field.performTextInput("y")

        field.assertTextContains(maximumText)
        composeRule.onNodeWithText("Characters: 100/100").assertExists()
    }

    @Test
    fun saveReturnsEditedTextForTheActiveCell() {
        var confirmedIndex: Int? = null
        var confirmedText: String? = null
        composeRule.setContent {
            AppTheme {
                EditorPane(
                    index = 3,
                    currentText = "",
                    onConfirm = { index, text ->
                        confirmedIndex = index
                        confirmedText = text
                    },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag(EDITOR_FIELD_TAG).performTextInput("edited")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle {
            assertEquals(3, confirmedIndex)
            assertEquals("edited", confirmedText)
        }
    }

    @Test
    fun cancelInvokesDismiss() {
        var dismissed = false
        composeRule.setContent {
            AppTheme {
                EditorPane(
                    index = 0,
                    currentText = "value",
                    onConfirm = { _, _ -> },
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.runOnIdle { assertTrue(dismissed) }
    }
}
