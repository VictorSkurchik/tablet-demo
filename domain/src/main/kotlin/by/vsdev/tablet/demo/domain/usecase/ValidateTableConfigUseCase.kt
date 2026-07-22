package by.vsdev.tablet.demo.domain.usecase

import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableLimits

enum class FieldError { EMPTY, NOT_A_NUMBER, BELOW_MIN, ABOVE_MAX }

data class TableConfigValidation(
    val rowsError: FieldError?,
    val columnsError: FieldError?,
    val config: TableConfig?,
) {
    val isValid: Boolean get() = config != null
}

class ValidateTableConfigUseCase {
    operator fun invoke(
        rowsInput: String,
        columnsInput: String,
    ): TableConfigValidation {
        val rowsError = validate(rowsInput, TableLimits.rowRange)
        val columnsError = validate(columnsInput, TableLimits.columnRange)
        val config =
            if (rowsError == null && columnsError == null) {
                TableConfig(rows = rowsInput.trim().toInt(), columns = columnsInput.trim().toInt())
            } else {
                null
            }
        return TableConfigValidation(rowsError, columnsError, config)
    }

    private fun validate(
        input: String,
        range: IntRange,
    ): FieldError? {
        val trimmed = input.trim()
        val value = trimmed.toIntOrNull()
        return when {
            trimmed.isEmpty() -> FieldError.EMPTY
            value == null -> FieldError.NOT_A_NUMBER
            value < range.first -> FieldError.BELOW_MIN
            value > range.last -> FieldError.ABOVE_MAX
            else -> null
        }
    }
}
