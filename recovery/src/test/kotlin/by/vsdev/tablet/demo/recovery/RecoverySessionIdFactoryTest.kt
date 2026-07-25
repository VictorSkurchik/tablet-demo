package by.vsdev.tablet.demo.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class RecoverySessionIdFactoryTest {
    @Test
    fun `UUID factory creates valid unique session IDs`() {
        val factory = UuidRecoverySessionIdFactory()

        val first = factory.newSessionId()
        val second = factory.newSessionId()

        assertTrue(isValidRecoverySessionId(first))
        assertTrue(isValidRecoverySessionId(second))
        assertEquals(first, UUID.fromString(first).toString())
        assertEquals(second, UUID.fromString(second).toString())
        assertNotEquals(first, second)
    }

    @Test
    fun `restore keeps valid session ID without generating a new one`() {
        var generationCount = 0
        val factory =
            RecoverySessionIdFactory {
                generationCount += 1
                "new-session"
            }

        val restored = factory.restoreOrCreate("existing_session-1")

        assertEquals("existing_session-1", restored)
        assertEquals(0, generationCount)
    }

    @Test
    fun `restore replaces missing or invalid session ID`() {
        var generationCount = 0
        val factory =
            RecoverySessionIdFactory {
                generationCount += 1
                "new-session-$generationCount"
            }

        assertEquals("new-session-1", factory.restoreOrCreate(null))
        assertEquals("new-session-2", factory.restoreOrCreate("../invalid"))
        assertEquals(2, generationCount)
    }

    @Test(expected = IllegalStateException::class)
    fun `restore rejects invalid ID produced by factory`() {
        RecoverySessionIdFactory { "../invalid" }.restoreOrCreate(null)
    }
}
