package by.vsdev.tablet.demo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TableConfigTest {
    @Test
    fun `boundary values are accepted and cell count is calculated`() {
        val minimum = TableConfig(rows = TableLimits.MIN_ROWS, columns = TableLimits.MIN_COLUMNS)
        val maximum = TableConfig(rows = TableLimits.MAX_ROWS, columns = TableLimits.MAX_COLUMNS)

        assertEquals(TableLimits.MIN_ROWS * TableLimits.MIN_COLUMNS, minimum.cellCount)
        assertEquals(TableLimits.MAX_ROWS * TableLimits.MAX_COLUMNS, maximum.cellCount)
    }

    @Test
    fun `row count outside limits is rejected`() {
        listOf(TableLimits.MIN_ROWS - 1, TableLimits.MAX_ROWS + 1).forEach { rows ->
            assertThrows(IllegalArgumentException::class.java) {
                TableConfig(rows = rows, columns = TableLimits.MIN_COLUMNS)
            }
        }
    }

    @Test
    fun `column count outside limits is rejected`() {
        listOf(TableLimits.MIN_COLUMNS - 1, TableLimits.MAX_COLUMNS + 1).forEach { columns ->
            assertThrows(IllegalArgumentException::class.java) {
                TableConfig(rows = TableLimits.MIN_ROWS, columns = columns)
            }
        }
    }
}
