package by.vsdev.tablet.demo.domain.repository

import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableData

interface TableDataRepository {
    suspend fun generate(config: TableConfig): TableData
}
