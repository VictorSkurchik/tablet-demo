package by.vsdev.tablet.demo.ui.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import by.vsdev.tablet.demo.ui.components.molecules.NumberField
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

class NumberFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rowsInputAcceptsOnlyFourDigits() {
        setNumberField(maxLength = 4)
        val field = composeRule.onNodeWithTag(NUMBER_FIELD_TAG)

        field.performTextInput("12a3")
        field.assertTextContains("123")
        field.performTextInput("4")
        field.assertTextContains("1234")
        field.performTextInput("5")

        field.assertTextContains("1234")
    }

    @Test
    fun invalidFieldExposesLocalizedErrorMessage() {
        setNumberField(
            maxLength = 4,
            isError = true,
            supportingText = "Maximum is 1000",
        )

        composeRule
            .onNodeWithTag(NUMBER_FIELD_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "Maximum is 1000",
                ),
            )
    }

    private fun setNumberField(
        maxLength: Int,
        isError: Boolean = false,
        supportingText: String = "Supporting text",
    ) {
        composeRule.setContent {
            AppTheme {
                NumberField(
                    state = TextFieldState(),
                    label = "Number",
                    supportingText = supportingText,
                    isError = isError,
                    maxLength = maxLength,
                    modifier = Modifier.testTag(NUMBER_FIELD_TAG),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val NUMBER_FIELD_TAG = "numberField"
    }
}
