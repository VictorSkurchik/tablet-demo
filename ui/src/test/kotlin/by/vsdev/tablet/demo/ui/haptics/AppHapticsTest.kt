package by.vsdev.tablet.demo.ui.haptics

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

class AppHapticsTest {
    private val performedTypes = mutableListOf<HapticFeedbackType>()
    private val appHaptics =
        ComposeAppHaptics(
            hapticFeedback =
                object : HapticFeedback {
                    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                        performedTypes += hapticFeedbackType
                    }
                },
        )

    @Test
    fun cellSelection_usesToggleFeedbackMatchingNewState() {
        appHaptics.performCellSelection(isSelected = true)
        appHaptics.performCellSelection(isSelected = false)

        assertEquals(
            listOf(HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOff),
            performedTypes,
        )
    }

    @Test
    fun cellEdit_usesContextClickFeedback() {
        appHaptics.performCellEdit()

        assertEquals(listOf(HapticFeedbackType.ContextClick), performedTypes)
    }
}
