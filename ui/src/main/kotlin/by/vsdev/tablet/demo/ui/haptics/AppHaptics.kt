package by.vsdev.tablet.demo.ui.haptics

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

internal interface AppHaptics {
    fun performCellSelection(isSelected: Boolean)

    fun performCellEdit()
}

private object NoOpAppHaptics : AppHaptics {
    override fun performCellSelection(isSelected: Boolean) = Unit

    override fun performCellEdit() = Unit
}

internal val LocalAppHaptics: ProvidableCompositionLocal<AppHaptics> =
    staticCompositionLocalOf { NoOpAppHaptics }

internal class ComposeAppHaptics(
    private val hapticFeedback: HapticFeedback,
) : AppHaptics {
    override fun performCellSelection(isSelected: Boolean) {
        hapticFeedback.performHapticFeedback(
            if (isSelected) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
        )
    }

    override fun performCellEdit() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
    }
}
