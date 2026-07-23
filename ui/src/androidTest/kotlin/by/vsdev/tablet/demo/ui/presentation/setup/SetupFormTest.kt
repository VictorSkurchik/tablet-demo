package by.vsdev.tablet.demo.ui.presentation.setup

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import by.vsdev.tablet.demo.domain.usecase.FieldError
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupFormTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactLayoutKeepsValidationErrorsAndBuildAction() {
        setCompactForm(
            state =
                SetupUiState(
                    rowsError = FieldError.ABOVE_MAX,
                    columnsError = FieldError.BELOW_MIN,
                ),
        )

        composeRule.onNodeWithText("Maximum is 1000").assertExists()
        composeRule.onNodeWithText("Minimum is 1").assertExists()
        composeRule.onNodeWithText("Build table").assertExists()
        composeRule.onNodeWithText("Build table").assertIsNotEnabled()
        composeRule.onNodeWithText("Table size").assertDoesNotExist()
    }

    @Test
    fun compactLayoutHidesOnlyHelperTextAndKeepsBuildClickable() {
        var buildClicked = false
        setCompactForm(
            state = SetupUiState(canBuild = true),
            rows = "4",
            columns = "3",
            onIntent = { buildClicked = it == SetupIntent.BuildClicked },
        )

        composeRule.onNodeWithText("Allowed: 1–1000").assertDoesNotExist()
        composeRule.onNodeWithText("Allowed: 1–6").assertDoesNotExist()
        composeRule.onNodeWithText("Build table").performClick()

        composeRule.runOnIdle { assertTrue(buildClicked) }
    }

    @Test
    fun keyboardTraversalMovesFromRowsToColumns() {
        setCompactForm(state = SetupUiState())
        val rows = composeRule.onNode(hasSetTextAction() and hasText("Rows"))
        val columns = composeRule.onNode(hasSetTextAction() and hasText("Columns"))

        rows.performSemanticsAction(SemanticsActions.RequestFocus)
        rows.performKeyInput { pressKey(Key.Tab) }

        columns.assertIsFocused()
    }

    @Test
    fun largeFontScaleKeepsPrimaryActionReachable() {
        var buildClicked = false
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 3f)) {
                AppTheme {
                    SetupForm(
                        state = SetupUiState(canBuild = true),
                        rowsInput = TextFieldState("4"),
                        columnsInput = TextFieldState("3"),
                        onIntent = { buildClicked = true },
                        useCompactImeLayout = false,
                    )
                }
            }
        }

        composeRule.onNodeWithText("Build table").performScrollTo().performClick()

        composeRule.runOnIdle { assertTrue(buildClicked) }
    }

    private fun setCompactForm(
        state: SetupUiState,
        rows: String = "9999",
        columns: String = "0",
        onIntent: (SetupIntent) -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                SetupForm(
                    state = state,
                    rowsInput = TextFieldState(rows),
                    columnsInput = TextFieldState(columns),
                    onIntent = onIntent,
                    useCompactImeLayout = true,
                )
            }
        }
    }
}
