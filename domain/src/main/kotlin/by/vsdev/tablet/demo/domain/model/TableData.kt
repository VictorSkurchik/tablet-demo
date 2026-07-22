package by.vsdev.tablet.demo.domain.model

/** Table content whose [cells] are stored in row-major order. */
data class TableData(
    val config: TableConfig,
    val cells: List<String>,
) {
    init {
        require(cells.size == config.cellCount) {
            "cells size ${cells.size} must equal config.cellCount (${config.cellCount})"
        }
    }

    fun cellAt(
        row: Int,
        column: Int,
    ): String {
        require(row in 0 until config.rows) { "row $row is outside 0 until ${config.rows}" }
        require(column in 0 until config.columns) {
            "column $column is outside 0 until ${config.columns}"
        }
        return cells[row * config.columns + column]
    }
}
