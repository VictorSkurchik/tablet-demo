package by.vsdev.tablet.demo.domain.usecase

import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableData
import by.vsdev.tablet.demo.domain.repository.TableDataRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GenerateTableDataUseCaseTest {
    private class FakeRepository(
        private val result: TableData,
    ) : TableDataRepository {
        var lastConfig: TableConfig? = null

        override suspend fun generate(config: TableConfig): TableData {
            lastConfig = config
            return result
        }
    }

    @Test
    fun `delegates config to the repository and returns its result`() =
        runTest {
            val config = TableConfig(rows = 4, columns = 2)
            val expected = TableData(config, List(config.cellCount) { "cell-$it" })
            val repository = FakeRepository(expected)
            val generate = GenerateTableDataUseCase(repository)

            val result = generate(config)

            assertEquals(config, repository.lastConfig)
            assertSame(expected, result)
        }
}
