package by.vsdev.tablet.demo.domain.model

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/** Table content whose [cells] are stored in row-major order. */
data class TableData(
    val config: TableConfig,
    val cells: PersistentList<String>,
) {
    constructor(
        config: TableConfig,
        cells: List<String>,
    ) : this(config, cells.toPersistentList())

    init {
        require(cells.size == config.cellCount) {
            "cells size ${cells.size} must equal config.cellCount (${config.cellCount})"
        }
    }
}
