package by.vsdev.tablet.demo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TableDataTest {
    @Test
    fun `source mutations cannot change table data`() {
        val source = mutableListOf("first", "second")
        val data = TableData(TableConfig(rows = 1, columns = 2), source)

        source[0] = "changed"

        assertEquals(listOf("first", "second"), data.cells)
    }

    @Test
    fun `mismatched cell count is rejected`() {
        val config = TableConfig(rows = 2, columns = 2)

        listOf(3, 5).forEach { size ->
            assertThrows(IllegalArgumentException::class.java) {
                TableData(config, List(size) { "cell-$it" })
            }
        }
    }
}
