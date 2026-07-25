package by.vsdev.tablet.demo.data.recovery

import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableLimits
import by.vsdev.tablet.demo.recovery.model.MAX_RECOVERED_CELL_VALUE_LENGTH
import by.vsdev.tablet.demo.recovery.model.RecoveredCell
import by.vsdev.tablet.demo.recovery.model.TableRecoverySnapshot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

internal object TableRecoverySnapshotCodec {
    private const val MAGIC = 0x54424C54
    private const val VERSION = 1
    private const val HEADER_SIZE = Int.SIZE_BYTES * 3
    private const val CHECKSUM_SIZE = Long.SIZE_BYTES
    private const val MAX_PAYLOAD_BYTES = 3 * 1024 * 1024
    private const val MAX_UTF8_BYTES_PER_CHARACTER = 4
    internal const val MAX_ENCODED_BYTES = HEADER_SIZE + MAX_PAYLOAD_BYTES + CHECKSUM_SIZE

    fun encode(snapshot: TableRecoverySnapshot): ByteArray {
        val payload =
            ByteArrayOutputStream().use { bytes ->
                DataOutputStream(bytes).use { output ->
                    output.writeInt(snapshot.config.rows)
                    output.writeInt(snapshot.config.columns)
                    output.writeInt(snapshot.cells.size)
                    snapshot.cells.forEach { cell ->
                        output.writeUtf8(cell.text)
                        output.writeBoolean(cell.isSelected)
                    }
                    output.writeInt(snapshot.editingIndex ?: -1)
                    snapshot.editorDraft?.let { output.writeUtf8(it) }
                }
                bytes.toByteArray()
            }
        require(payload.size <= MAX_PAYLOAD_BYTES) { "recovery snapshot is too large" }

        val checksum = CRC32().apply { update(payload) }.value
        return ByteBuffer
            .allocate(HEADER_SIZE + payload.size + CHECKSUM_SIZE)
            .putInt(MAGIC)
            .putInt(VERSION)
            .putInt(payload.size)
            .put(payload)
            .putLong(checksum)
            .array()
    }

    fun decode(bytes: ByteArray): TableRecoverySnapshot? =
        try {
            require(bytes.size in (HEADER_SIZE + CHECKSUM_SIZE)..MAX_ENCODED_BYTES)
            DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                require(input.readInt() == MAGIC)
                require(input.readInt() == VERSION)
                val payloadSize = input.readInt()
                require(payloadSize in 1..MAX_PAYLOAD_BYTES)
                require(payloadSize == bytes.size - HEADER_SIZE - CHECKSUM_SIZE)

                val payload = ByteArray(payloadSize)
                input.readFully(payload)
                val expectedChecksum = input.readLong()
                val actualChecksum = CRC32().apply { update(payload) }.value
                require(expectedChecksum == actualChecksum)
                require(input.read() == -1)
                decodePayload(payload)
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun decodePayload(payload: ByteArray): TableRecoverySnapshot =
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            val rows = input.readInt()
            val columns = input.readInt()
            require(rows in TableLimits.rowRange)
            require(columns in TableLimits.columnRange)
            val config = TableConfig(rows, columns)
            val cellCount = input.readInt()
            require(cellCount == config.cellCount)
            val cells =
                List(cellCount) {
                    RecoveredCell(
                        text = input.readUtf8(MAX_RECOVERED_CELL_VALUE_LENGTH),
                        isSelected = input.readBoolean(),
                    )
                }
            val editingIndex = input.readInt().takeUnless { it == -1 }
            val editorDraft =
                if (editingIndex == null) {
                    null
                } else {
                    input.readUtf8(MAX_RECOVERED_CELL_VALUE_LENGTH)
                }
            require(input.read() == -1)
            TableRecoverySnapshot(config, cells, editingIndex, editorDraft)
        }

    private fun DataOutputStream.writeUtf8(value: String) {
        val encoded = value.toByteArray(StandardCharsets.UTF_8)
        writeInt(encoded.size)
        write(encoded)
    }

    private fun DataInputStream.readUtf8(maxCharacters: Int): String {
        val byteCount = readInt()
        require(byteCount in 0..(maxCharacters * MAX_UTF8_BYTES_PER_CHARACTER))
        val encoded = ByteArray(byteCount)
        readFully(encoded)
        val decoded =
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(encoded))
                .toString()
        require(decoded.length <= maxCharacters)
        return decoded
    }
}
