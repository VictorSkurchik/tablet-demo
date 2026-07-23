package by.vsdev.tablet.demo.ui.components.molecules

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics

@Composable
internal fun AppOutlinedTextField(
    state: TextFieldState,
    label: String,
    maxLength: Int,
    modifier: Modifier = Modifier,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: () -> Unit = {},
    inputTransformation: InputTransformation? = null,
) {
    val lengthFilter = remember(maxLength) { InputTransformation.maxLength(maxLength) }
    val combinedTransformation =
        remember(inputTransformation, lengthFilter) {
            inputTransformation?.then(lengthFilter) ?: lengthFilter
        }

    OutlinedTextField(
        state = state,
        label = { Text(label) },
        supportingText = supportingText,
        isError = isError,
        inputTransformation = combinedTransformation,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = { onKeyboardAction() },
        modifier =
            modifier.semantics {
                errorMessage?.let { error(it) }
            },
    )
}
