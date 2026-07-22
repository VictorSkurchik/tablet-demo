package by.vsdev.tablet.demo.ui.presentation.setup

import by.vsdev.tablet.demo.domain.usecase.FieldError

internal data class SetupUiState(
    val rowsError: FieldError? = null,
    val columnsError: FieldError? = null,
    val canBuild: Boolean = false,
)

internal sealed interface SetupIntent {
    data object BuildClicked : SetupIntent
}
