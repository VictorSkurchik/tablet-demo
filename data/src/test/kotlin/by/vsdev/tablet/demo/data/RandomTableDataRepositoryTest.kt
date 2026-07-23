package by.vsdev.tablet.demo.data

import by.vsdev.tablet.demo.data.random.RandomStringGenerator
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableData
import by.vsdev.tablet.demo.domain.model.TableDataResult
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class RandomTableDataRepositoryTest {
    private fun repository(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        RandomTableDataRepository(
            BackgroundDispatcher(StandardTestDispatcher(scheduler)),
            RandomStringGenerator(Random(1L)),
        )

    @Test
    fun `returns generated cells for the requested config`() =
        runTest {
            val config = TableConfig(rows = 2, columns = 2)
            val expectedGenerator = RandomStringGenerator(Random(1L))
            val expected = TableData(config, List(config.cellCount) { expectedGenerator.generate() })

            val result = repository(testScheduler).generate(config)

            assertEquals(TableDataResult.Success(expected), result)
        }
}
