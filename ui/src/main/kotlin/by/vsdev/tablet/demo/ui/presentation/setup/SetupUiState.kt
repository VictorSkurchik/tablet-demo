package by.vsdev.tablet.demo.ui.presentation.setup

import androidx.compose.runtime.Immutable
import by.vsdev.tablet.demo.domain.usecase.FieldError

@Immutable
internal data class SetupUiState(
    val rowsError: FieldError? = null,
    val columnsError: FieldError? = null,
    val canBuild: Boolean = false,
)
