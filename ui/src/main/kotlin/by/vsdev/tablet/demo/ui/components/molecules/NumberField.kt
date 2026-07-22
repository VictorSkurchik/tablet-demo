package by.vsdev.tablet.demo.ui.components.molecules

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.byValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import by.vsdev.tablet.demo.ui.theme.AppTheme

@Composable
internal fun NumberField(
    state: TextFieldState,
    label: String,
    supportingText: String,
    isError: Boolean,
    maxLength: Int,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    val digitsOnly =
        remember {
            InputTransformation.byValue { _, proposed -> proposed.filter(Char::isDigit) }
        }

    AppOutlinedTextField(
        state = state,
        label = label,
        maxLength = maxLength,
        isError = isError,
        supportingText = { Text(supportingText) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        onKeyboardAction = onImeAction,
        inputTransformation = digitsOnly,
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview
@Composable
private fun NumberFieldPreview() {
    AppTheme {
        NumberField(
            state = TextFieldState("42"),
            label = "Rows",
            supportingText = "Allowed: 1–1000",
            isError = false,
            maxLength = 4,
        )
    }
}

@Preview
@Composable
private fun NumberFieldErrorPreview() {
    AppTheme {
        NumberField(
            state = TextFieldState("9999"),
            label = "Rows",
            supportingText = "Maximum is 1000",
            isError = true,
            maxLength = 4,
        )
    }
}
