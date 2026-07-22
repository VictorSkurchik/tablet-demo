package by.vsdev.tablet.demo.domain.usecase

import by.vsdev.tablet.demo.domain.model.TableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTableConfigUseCaseTest {
    private val validate = ValidateTableConfigUseCase()

    @Test
    fun `valid inputs produce a config`() {
        val result = validate("500", "3")

        assertTrue(result.isValid)
        assertEquals(TableConfig(rows = 500, columns = 3), result.config)
        assertNull(result.rowsError)
        assertNull(result.columnsError)
    }

    @Test
    fun `boundary values are accepted`() {
        val min = validate("1", "1")
        val max = validate("1000", "6")

        assertEquals(TableConfig(1, 1), min.config)
        assertEquals(TableConfig(1000, 6), max.config)
    }

    @Test
    fun `empty inputs are flagged and yield no config`() {
        val result = validate("", "  ")

        assertFalse(result.isValid)
        assertNull(result.config)
        assertEquals(FieldError.EMPTY, result.rowsError)
        assertEquals(FieldError.EMPTY, result.columnsError)
    }

    @Test
    fun `non-numeric input is flagged`() {
        val result = validate("12a", "3")
        assertEquals(FieldError.NOT_A_NUMBER, result.rowsError)
        assertNull(result.columnsError)
    }

    @Test
    fun `values below minimum are flagged`() {
        val result = validate("0", "0")
        assertEquals(FieldError.BELOW_MIN, result.rowsError)
        assertEquals(FieldError.BELOW_MIN, result.columnsError)
    }

    @Test
    fun `rows above 1000 and columns above 6 are flagged`() {
        val result = validate("1001", "7")
        assertEquals(FieldError.ABOVE_MAX, result.rowsError)
        assertEquals(FieldError.ABOVE_MAX, result.columnsError)
    }

    @Test
    fun `surrounding whitespace is tolerated`() {
        val result = validate("  10  ", " 2 ")
        assertEquals(TableConfig(10, 2), result.config)
    }

    @Test
    fun `one invalid field blocks the whole config`() {
        val result = validate("10", "9")
        assertNull(result.config)
        assertNull(result.rowsError)
        assertEquals(FieldError.ABOVE_MAX, result.columnsError)
    }
}
