package by.vsdev.tablet.demo.ui.components.molecules

import androidx.compose.ui.input.InputMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectableCellFocusIndicatorTest {
    @Test
    fun `focused cell does not show focus indicator in touch mode`() {
        assertFalse(
            shouldShowCellFocusIndicator(
                focused = true,
                inputMode = InputMode.Touch,
            ),
        )
    }

    @Test
    fun `focused cell shows focus indicator in keyboard mode`() {
        assertTrue(
            shouldShowCellFocusIndicator(
                focused = true,
                inputMode = InputMode.Keyboard,
            ),
        )
    }
}
