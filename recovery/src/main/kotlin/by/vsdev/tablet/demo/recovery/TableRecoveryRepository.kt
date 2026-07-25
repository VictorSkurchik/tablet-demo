package by.vsdev.tablet.demo.recovery

import by.vsdev.tablet.demo.recovery.model.TableRecoverySnapshot

interface TableRecoveryRepository {
    suspend fun load(sessionId: String): TableRecoverySnapshot?

    suspend fun save(
        sessionId: String,
        snapshot: TableRecoverySnapshot,
    ): Boolean

    suspend fun delete(sessionId: String): Boolean

    suspend fun cleanupExpired(): Boolean
}

enum class RecoveryOperation {
    LOAD,
    SAVE,
    DELETE,
    CLEANUP,
}

fun interface RecoveryFailureReporter {
    fun report(
        operation: RecoveryOperation,
        sessionId: String?,
        error: Throwable,
    )
}

private val validRecoverySessionId = Regex("[A-Za-z0-9_-]{1,64}")

fun isValidRecoverySessionId(value: String?): Boolean = value != null && validRecoverySessionId.matches(value)
