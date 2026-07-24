package by.vsdev.tablet.demo.data.random

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RandomStringGeneratorTest {
    @Test
    fun `produces strings within the expected length bounds`() {
        val generator = RandomStringGenerator(Random(0L))

        repeat(1000) {
            val value = generator.generate()
            assertTrue("length ${value.length} out of bounds", value.length in 5..10)
            assertTrue("unexpected characters in $value", value.all { it in ASCII_ALPHANUMERIC })
        }
    }

    private companion object {
        const val ASCII_ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }
}
