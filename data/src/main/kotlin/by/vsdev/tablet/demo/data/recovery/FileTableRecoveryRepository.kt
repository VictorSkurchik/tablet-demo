package by.vsdev.tablet.demo.data.recovery

import by.vsdev.tablet.demo.recovery.RecoveryFailureReporter
import by.vsdev.tablet.demo.recovery.RecoveryOperation
import by.vsdev.tablet.demo.recovery.TableRecoveryRepository
import by.vsdev.tablet.demo.recovery.isValidRecoverySessionId
import by.vsdev.tablet.demo.recovery.model.TableRecoverySnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private const val SNAPSHOT_SUFFIX = ".snapshot"
private const val TEMP_SUFFIX = ".tmp"
private const val DEFAULT_RETENTION_MILLIS = 24 * 60 * 60 * 1000L

class FileTableRecoveryRepository(
    private val directory: File,
    private val ioDispatcher: RecoveryIoDispatcher,
    private val failureReporter: RecoveryFailureReporter,
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val retentionMillis: Long = DEFAULT_RETENTION_MILLIS,
) : TableRecoveryRepository {
    private val mutex = Mutex()

    override suspend fun load(sessionId: String): TableRecoverySnapshot? {
        val file = snapshotFile(sessionId)
        return try {
            onDisk {
                if (!file.isFile || file.length() > TableRecoverySnapshotCodec.MAX_ENCODED_BYTES) {
                    deleteCorruptFile(file, sessionId)
                    return@onDisk null
                }
                val snapshot = TableRecoverySnapshotCodec.decode(file.readBytes())
                if (snapshot == null) deleteCorruptFile(file, sessionId)
                snapshot
            }
        } catch (error: IOException) {
            failureReporter.report(RecoveryOperation.LOAD, sessionId, error)
            null
        } catch (error: SecurityException) {
            failureReporter.report(RecoveryOperation.LOAD, sessionId, error)
            null
        }
    }

    override suspend fun save(
        sessionId: String,
        snapshot: TableRecoverySnapshot,
    ): Boolean {
        val target = snapshotFile(sessionId)
        return try {
            onDisk {
                ensureDirectory()
                val temporary = File(directory, "$sessionId$TEMP_SUFFIX")
                try {
                    FileOutputStream(temporary).use { output ->
                        output.write(TableRecoverySnapshotCodec.encode(snapshot))
                        output.fd.sync()
                    }
                    moveAtomically(temporary, target)
                } finally {
                    temporary.delete()
                }
            }
            true
        } catch (error: IOException) {
            failureReporter.report(RecoveryOperation.SAVE, sessionId, error)
            false
        } catch (error: SecurityException) {
            failureReporter.report(RecoveryOperation.SAVE, sessionId, error)
            false
        } catch (error: IllegalStateException) {
            failureReporter.report(RecoveryOperation.SAVE, sessionId, error)
            false
        }
    }

    override suspend fun delete(sessionId: String): Boolean {
        val snapshot = snapshotFile(sessionId)
        return try {
            onDisk {
                val snapshotDeleted = !snapshot.exists() || snapshot.delete()
                val temporary = File(directory, "$sessionId$TEMP_SUFFIX")
                val temporaryDeleted = !temporary.exists() || temporary.delete()
                val allDeleted = snapshotDeleted && temporaryDeleted
                if (!allDeleted) {
                    failureReporter.report(
                        RecoveryOperation.DELETE,
                        sessionId,
                        IOException("Could not delete all recovery artifacts"),
                    )
                }
                allDeleted
            }
        } catch (error: SecurityException) {
            failureReporter.report(RecoveryOperation.DELETE, sessionId, error)
            false
        }
    }

    override suspend fun cleanupExpired(): Boolean =
        try {
            onDisk {
                val cutoff = clockMillis() - retentionMillis
                var allDeleted = true
                directory
                    .listFiles()
                    .orEmpty()
                    .filter {
                        (it.name.endsWith(SNAPSHOT_SUFFIX) || it.name.endsWith(TEMP_SUFFIX)) &&
                            it.lastModified() < cutoff
                    }.forEach { file ->
                        if (!file.delete()) {
                            allDeleted = false
                            failureReporter.report(
                                RecoveryOperation.CLEANUP,
                                null,
                                IOException("Could not delete stale artifact ${file.name}"),
                            )
                        }
                    }
                allDeleted
            }
        } catch (error: SecurityException) {
            failureReporter.report(RecoveryOperation.CLEANUP, null, error)
            false
        }

    private suspend fun <T> onDisk(block: () -> T): T =
        withContext(ioDispatcher.value) {
            mutex.withLock { block() }
        }

    private fun snapshotFile(sessionId: String): File {
        require(isValidRecoverySessionId(sessionId)) { "Invalid recovery session ID" }
        return File(directory, "$sessionId$SNAPSHOT_SUFFIX")
    }

    private fun ensureDirectory() {
        check(directory.isDirectory || directory.mkdirs()) {
            "Could not create recovery directory"
        }
    }

    private fun deleteCorruptFile(
        file: File,
        sessionId: String,
    ) {
        if (file.exists() && !file.delete()) {
            failureReporter.report(
                RecoveryOperation.DELETE,
                sessionId,
                IOException("Could not delete invalid recovery snapshot"),
            )
        }
    }

    private fun moveAtomically(
        source: File,
        target: File,
    ) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (_: UnsupportedOperationException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
