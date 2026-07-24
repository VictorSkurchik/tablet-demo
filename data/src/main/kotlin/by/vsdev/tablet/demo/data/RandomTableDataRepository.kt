package by.vsdev.tablet.demo.data

import by.vsdev.tablet.demo.data.random.RandomStringGenerator
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableData
import by.vsdev.tablet.demo.domain.repository.TableDataRepository
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.withContext

internal class RandomTableDataRepository(
    private val backgroundDispatcher: BackgroundDispatcher,
    private val stringGenerator: RandomStringGenerator,
) : TableDataRepository {
    override suspend fun generate(config: TableConfig): TableData =
        withContext(backgroundDispatcher.value) {
            val cells = persistentListOf<String>().builder()
            repeat(config.cellCount) {
                cells.add(stringGenerator.generate())
            }
            TableData(config, cells.build())
        }
}
