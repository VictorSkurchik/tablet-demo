package by.vsdev.tablet.demo.domain.model

object TableLimits {
    const val MIN_ROWS = 1
    const val MAX_ROWS = 1000
    const val MIN_COLUMNS = 1
    const val MAX_COLUMNS = 6

    val rowRange: IntRange = MIN_ROWS..MAX_ROWS
    val columnRange: IntRange = MIN_COLUMNS..MAX_COLUMNS
}

data class TableConfig(
    val rows: Int,
    val columns: Int,
) {
    init {
        require(rows in TableLimits.rowRange) {
            "rows must be in ${TableLimits.rowRange}, was $rows"
        }
        require(columns in TableLimits.columnRange) {
            "columns must be in ${TableLimits.columnRange}, was $columns"
        }
    }

    val cellCount: Int get() = rows * columns
}
