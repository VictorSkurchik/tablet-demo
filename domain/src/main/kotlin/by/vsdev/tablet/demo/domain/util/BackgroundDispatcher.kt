package by.vsdev.tablet.demo.domain.util

import kotlinx.coroutines.CoroutineDispatcher

@JvmInline
value class BackgroundDispatcher(
    val value: CoroutineDispatcher,
)
