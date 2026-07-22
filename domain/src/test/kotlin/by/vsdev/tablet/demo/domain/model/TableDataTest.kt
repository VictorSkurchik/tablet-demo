package by.vsdev.tablet.demo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TableDataTest {
    @Test
    fun `cellAt resolves row-major indices`() {
        val data =
            TableData(
                config = TableConfig(rows = 2, columns = 3),
                cells = listOf("00", "01", "02", "10", "11", "12"),
            )

        assertEquals("00", data.cellAt(0, 0))
        assertEquals("02", data.cellAt(0, 2))
        assertEquals("10", data.cellAt(1, 0))
        assertEquals("12", data.cellAt(1, 2))
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

    @Test
    fun `row outside config is rejected`() {
        val data = TableData(TableConfig(rows = 1, columns = 1), listOf("cell"))

        listOf(-1, 1).forEach { row ->
            assertThrows(IllegalArgumentException::class.java) { data.cellAt(row, 0) }
        }
    }

    @Test
    fun `column outside config is rejected`() {
        val data = TableData(TableConfig(rows = 1, columns = 1), listOf("cell"))

        listOf(-1, 1).forEach { column ->
            assertThrows(IllegalArgumentException::class.java) { data.cellAt(0, column) }
        }
    }
}
