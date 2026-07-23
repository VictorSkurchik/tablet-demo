package by.vsdev.tablet.demo.ui.presentation.table.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import by.vsdev.tablet.demo.ui.R
import by.vsdev.tablet.demo.ui.components.molecules.AppOutlinedTextField
import by.vsdev.tablet.demo.ui.presentation.table.MAX_CELL_TEXT_LENGTH
import by.vsdev.tablet.demo.ui.theme.AppSpacing
import by.vsdev.tablet.demo.ui.theme.AppTheme

internal const val EDITOR_FIELD_TAG = "editorField"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditorPane(
    index: Int?,
    currentText: String?,
    onConfirm: (Int, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (index == null || currentText == null) {
        EmptyEditorPane(modifier)
        return
    }

    val draft =
        rememberSaveable(index, saver = TextFieldState.Saver) {
            TextFieldState(currentText.take(MAX_CELL_TEXT_LENGTH))
        }
    val focusRequester = remember(index) { FocusRequester() }
    val fieldRequester = remember(index) { BringIntoViewRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isOnScreenKeyboardVisible = WindowInsets.isImeVisible

    LaunchedEffect(index) {
        focusRequester.requestFocus()
        withFrameNanos { }
        keyboardController?.show()
    }

    LaunchedEffect(index, isOnScreenKeyboardVisible) {
        if (isOnScreenKeyboardVisible) {
            withFrameNanos { }
            fieldRequester.bringIntoView()
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(AppSpacing.large),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        Text(
            stringResource(R.string.editor_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        EditorControls(
            index = index,
            draft = draft,
            focusRequester = focusRequester,
            fieldRequester = fieldRequester,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun EditorControls(
    index: Int,
    draft: TextFieldState,
    focusRequester: FocusRequester,
    fieldRequester: BringIntoViewRequester,
    onConfirm: (Int, String) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().focusGroup(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        AppOutlinedTextField(
            state = draft,
            label = stringResource(R.string.editor_field_label),
            maxLength = MAX_CELL_TEXT_LENGTH,
            supportingText = {
                Text(
                    stringResource(
                        R.string.editor_character_count,
                        draft.text.length,
                        MAX_CELL_TEXT_LENGTH,
                    ),
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = { onConfirm(index, draft.text.toString()) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(fieldRequester)
                    .testTag(EDITOR_FIELD_TAG)
                    .focusRequester(focusRequester),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_cancel)) }
            Button(onClick = { onConfirm(index, draft.text.toString()) }) {
                Text(stringResource(R.string.editor_save))
            }
        }
    }
}

@Composable
private fun EmptyEditorPane(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(AppSpacing.large),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.editor_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "Editor – active", widthDp = 280, heightDp = 420)
@Preview(name = "Editor – large font", widthDp = 280, heightDp = 520, fontScale = 1.5f)
@Composable
private fun EditorPanePreview() {
    AppTheme {
        EditorPane(
            index = 3,
            currentText = "Edited cell value",
            onConfirm = { _, _ -> },
            onDismiss = {},
        )
    }
}

@Preview(name = "Editor – empty", widthDp = 280, heightDp = 240)
@Composable
private fun EditorPaneEmptyPreview() {
    AppTheme { EditorPane(index = null, currentText = null, onConfirm = { _, _ -> }, onDismiss = {}) }
}
