package by.vsdev.tablet.demo.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinationsTest {
    @Test
    fun `legacy destination without recovery session remains decodable`() {
        val destination =
            Json.decodeFromString<TableDestination>(
                """{"rows":2,"columns":3}""",
            )

        assertEquals(2, destination.rows)
        assertEquals(3, destination.columns)
        assertNull(destination.recoverySessionId)
    }
}
