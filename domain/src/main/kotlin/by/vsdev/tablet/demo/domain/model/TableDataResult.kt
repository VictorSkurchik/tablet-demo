package by.vsdev.tablet.demo.domain.model

sealed interface TableDataResult {
    data class Success(
        val data: TableData,
    ) : TableDataResult

    data object GenerationUnavailable : TableDataResult
}
