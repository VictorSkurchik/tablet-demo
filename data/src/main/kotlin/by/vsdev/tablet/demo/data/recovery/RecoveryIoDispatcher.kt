package by.vsdev.tablet.demo.data.recovery

import kotlinx.coroutines.CoroutineDispatcher

@JvmInline
value class RecoveryIoDispatcher(
    val value: CoroutineDispatcher,
)
