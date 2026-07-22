package by.vsdev.tablet.demo.navigation

import by.vsdev.tablet.demo.domain.model.TableConfig
import kotlinx.serialization.Serializable

@Serializable
internal data object SetupDestination

@Serializable
internal data class TableDestination(
    val rows: Int,
    val columns: Int,
) {
    constructor(config: TableConfig) : this(
        rows = config.rows,
        columns = config.columns,
    )

    fun toConfig(): TableConfig =
        TableConfig(
            rows = rows,
            columns = columns,
        )
}
