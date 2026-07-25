package by.vsdev.tablet.demo.recovery

import java.util.UUID

fun interface RecoverySessionIdFactory {
    fun newSessionId(): String
}

class UuidRecoverySessionIdFactory : RecoverySessionIdFactory {
    override fun newSessionId(): String = UUID.randomUUID().toString()
}

fun RecoverySessionIdFactory.restoreOrCreate(savedSessionId: String?): String =
    savedSessionId
        ?.takeIf(::isValidRecoverySessionId)
        ?: newSessionId().also {
            check(isValidRecoverySessionId(it)) {
                "RecoverySessionIdFactory produced an invalid session ID"
            }
        }
