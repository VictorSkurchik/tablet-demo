package by.vsdev.tablet.demo.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {
    @Test
    fun `selected cell content meets normal text contrast in light theme`() {
        assertNormalTextContrast(LightCellColors.onSelected, LightCellColors.selected)
    }

    @Test
    fun `selected cell content meets normal text contrast in dark theme`() {
        assertNormalTextContrast(DarkCellColors.onSelected, DarkCellColors.selected)
    }

    private fun assertNormalTextContrast(
        foreground: Color,
        background: Color,
    ) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val ratio = (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)

        assertTrue(
            "Expected contrast >= $MINIMUM_NORMAL_TEXT_CONTRAST, was $ratio",
            ratio >= MINIMUM_NORMAL_TEXT_CONTRAST,
        )
    }

    private companion object {
        const val LUMINANCE_OFFSET = 0.05f
        const val MINIMUM_NORMAL_TEXT_CONTRAST = 4.5f
    }
}
