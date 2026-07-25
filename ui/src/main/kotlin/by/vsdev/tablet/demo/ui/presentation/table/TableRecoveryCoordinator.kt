package by.vsdev.tablet.demo.ui.presentation.table

import by.vsdev.tablet.demo.recovery.TableRecoveryRepository
import by.vsdev.tablet.demo.recovery.model.TableRecoverySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val PERSISTENCE_THROTTLE_MILLIS = 750L
private const val PERSISTENCE_WRITE_ATTEMPTS = 3
private const val PERSISTENCE_RETRY_DELAY_MILLIS = 250L
private const val NANOS_PER_MILLISECOND = 1_000_000L

private enum class PersistenceSignal {
    CHANGED,
    FLUSH,
}

/**
 * Owns the lifecycle and scheduling policy for a single transient recovery session.
 *
 * The coordinator runs in the ViewModel's main-thread scope so [snapshotProvider] observes a
 * consistent UI state. The repository is responsible for moving file operations to its IO
 * dispatcher.
 */
internal class TableRecoveryCoordinator(
    private val sessionId: String,
    private val repository: TableRecoveryRepository,
    private val scope: CoroutineScope,
    private val snapshotProvider: () -> TableRecoverySnapshot,
) {
    private val signals = Channel<PersistenceSignal>(Channel.CONFLATED)
    private var isClosed = false
    private var stateVersion = 0L
    private var persistedVersion = -1L
    private val persistenceJob: Job =
        scope.launch {
            for (signal in signals) {
                if (signal == PersistenceSignal.CHANGED) awaitThrottleOrFlush()
                drainPendingSignals()
                if (!isClosed) persistLatest()
            }
        }

    fun markDirty() {
        if (isClosed) return
        stateVersion++
        signals.trySend(PersistenceSignal.CHANGED)
    }

    fun flush() {
        if (!isClosed) signals.trySend(PersistenceSignal.FLUSH)
    }

    fun close(onClosed: () -> Unit) {
        if (isClosed) return
        isClosed = true
        scope.launch {
            signals.close()
            persistenceJob.cancelAndJoin()
            repository.delete(sessionId)
            onClosed()
        }
    }

    private suspend fun awaitThrottleOrFlush() {
        val deadlineNanos =
            System.nanoTime() + PERSISTENCE_THROTTLE_MILLIS * NANOS_PER_MILLISECOND
        var isWaiting = true
        while (isWaiting && !isClosed) {
            val remainingNanos = deadlineNanos - System.nanoTime()
            if (remainingNanos <= 0L) {
                isWaiting = false
            } else {
                val remainingMillis =
                    (remainingNanos / NANOS_PER_MILLISECOND)
                        .coerceAtLeast(1L)
                val next =
                    withTimeoutOrNull(remainingMillis) {
                        signals.receiveCatching().getOrNull()
                    }
                if (next == null || next == PersistenceSignal.FLUSH) isWaiting = false
            }
        }
    }

    private fun drainPendingSignals() {
        while (signals.tryReceive().isSuccess) {
            // The snapshot includes every mutation observed before this main-thread read.
        }
    }

    private suspend fun persistLatest() {
        if (persistedVersion == stateVersion) return
        val version = stateVersion
        val snapshot = snapshotProvider()
        repeat(PERSISTENCE_WRITE_ATTEMPTS) { attempt ->
            if (repository.save(sessionId, snapshot)) {
                persistedVersion = version
                return
            }
            if (attempt < PERSISTENCE_WRITE_ATTEMPTS - 1) {
                delay(PERSISTENCE_RETRY_DELAY_MILLIS * (attempt + 1))
            }
        }
    }
}
