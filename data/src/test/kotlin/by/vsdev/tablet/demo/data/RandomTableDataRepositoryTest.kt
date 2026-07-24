package by.vsdev.tablet.demo.data

import by.vsdev.tablet.demo.data.random.RandomStringGenerator
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableData
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class RandomTableDataRepositoryTest {
    @Test
    fun `generates the requested table on the injected dispatcher`() =
        runTest {
            val config = TableConfig(rows = 2, columns = 2)
            val expectedGenerator = RandomStringGenerator(Random(1L))
            val expected = TableData(config, List(config.cellCount) { expectedGenerator.generate() })
            val dispatcher = CountingDispatcher(StandardTestDispatcher(testScheduler))
            val repository =
                RandomTableDataRepository(
                    BackgroundDispatcher(dispatcher),
                    RandomStringGenerator(Random(1L)),
                )

            val result = repository.generate(config)

            assertEquals(expected, result)
            assertTrue(dispatcher.dispatchCount > 0)
        }

    private class CountingDispatcher(
        private val delegate: CoroutineDispatcher,
    ) : CoroutineDispatcher() {
        var dispatchCount = 0
            private set

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatchCount++
            delegate.dispatch(context, block)
        }
    }
}
