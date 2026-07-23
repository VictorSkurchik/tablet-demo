package by.vsdev.tablet.demo.data.recovery

import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.recovery.model.RecoveredCell
import by.vsdev.tablet.demo.recovery.model.TableRecoverySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableRecoverySnapshotCodecTest {
    @Test
    fun `round trip retains values selections and editor`() {
        val snapshot =
            TableRecoverySnapshot(
                config = TableConfig(2, 2),
                cells =
                    listOf(
                        RecoveredCell("original", false),
                        RecoveredCell("edited", true),
                        RecoveredCell("кириллица", false),
                        RecoveredCell("😀", true),
                    ),
                editingIndex = 1,
                editorDraft = "draft",
            )

        assertEquals(snapshot, TableRecoverySnapshotCodec.decode(TableRecoverySnapshotCodec.encode(snapshot)))
    }

    @Test
    fun `maximum table and values fit the bounded format`() {
        val config = TableConfig(1000, 6)
        val snapshot =
            TableRecoverySnapshot(
                config = config,
                cells = List(config.cellCount) { RecoveredCell("€".repeat(100), it % 2 == 0) },
            )

        val encoded = TableRecoverySnapshotCodec.encode(snapshot)

        assertTrue(encoded.size < TableRecoverySnapshotCodec.MAX_ENCODED_BYTES)
        assertEquals(snapshot, TableRecoverySnapshotCodec.decode(encoded))
    }

    @Test
    fun `corrupt checksum is rejected`() {
        val encoded = TableRecoverySnapshotCodec.encode(minimalSnapshot())
        encoded[encoded.lastIndex] = (encoded.last() + 1).toByte()

        assertNull(TableRecoverySnapshotCodec.decode(encoded))
    }

    @Test
    fun `partial snapshot is rejected`() {
        val encoded = TableRecoverySnapshotCodec.encode(minimalSnapshot())

        assertNull(TableRecoverySnapshotCodec.decode(encoded.copyOf(encoded.size / 2)))
    }

    @Test
    fun `unsupported version is rejected`() {
        val encoded = TableRecoverySnapshotCodec.encode(minimalSnapshot())
        encoded[7] = 2

        assertNull(TableRecoverySnapshotCodec.decode(encoded))
    }

    private fun minimalSnapshot(): TableRecoverySnapshot =
        TableRecoverySnapshot(
            config = TableConfig(1, 1),
            cells = listOf(RecoveredCell("value", false)),
        )
}
