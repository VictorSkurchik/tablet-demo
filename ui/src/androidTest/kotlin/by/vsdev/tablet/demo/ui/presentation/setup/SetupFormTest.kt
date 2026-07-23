package by.vsdev.tablet.demo.ui.presentation.setup

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeRule.waitForIdle()
    }
}
