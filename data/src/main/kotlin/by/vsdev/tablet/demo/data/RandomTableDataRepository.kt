package by.vsdev.tablet.demo.data

import by.vsdev.tablet.demo.data.random.RandomStringGenerator
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableData
import by.vsdev.tablet.demo.domain.model.TableDataResult
import by.vsdev.tablet.demo.domain.repository.TableDataRepository
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import kotlinx.coroutines.withContext

internal class RandomTableDataRepository(
    private val backgroundDispatcher: BackgroundDispatcher,
    private val stringGenerator: RandomStringGenerator,
) : TableDataRepository {
    override suspend fun generate(config: TableConfig): TableDataResult =
        withContext(backgroundDispatcher.value) {
            val cells = List(config.cellCount) { stringGenerator.generate() }
            TableDataResult.Success(TableData(config, cells))
        }
}
