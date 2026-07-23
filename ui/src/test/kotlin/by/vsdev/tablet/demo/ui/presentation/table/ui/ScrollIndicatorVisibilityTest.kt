package by.vsdev.tablet.demo.ui.presentation.table.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollIndicatorVisibilityTest {
    @Test
    fun `does not reserve indicator space when content fits`() {
        assertEquals(
            ScrollIndicatorVisibility(vertical = false, horizontal = false),
            visibility(contentWidth = 500f, contentHeight = 300f),
        )
    }

    @Test
    fun `shows only vertical indicator for tall content`() {
        assertEquals(
            ScrollIndicatorVisibility(vertical = true, horizontal = false),
            visibility(contentWidth = 500f, contentHeight = 700f),
        )
    }

    @Test
    fun `shows only horizontal indicator for wide content`() {
        assertEquals(
            ScrollIndicatorVisibility(vertical = false, horizontal = true),
            visibility(contentWidth = 900f, contentHeight = 300f),
        )
    }

    @Test
    fun `horizontal indicator can make vertical indicator necessary`() {
        assertEquals(
            ScrollIndicatorVisibility(vertical = true, horizontal = true),
            visibility(contentWidth = 900f, contentHeight = 590f),
        )
    }

    @Test
    fun `vertical indicator can make horizontal indicator necessary`() {
        assertEquals(
            ScrollIndicatorVisibility(vertical = true, horizontal = true),
            visibility(contentWidth = 790f, contentHeight = 700f),
        )
    }

    private fun visibility(
        contentWidth: Float,
        contentHeight: Float,
    ) = calculateScrollIndicatorVisibility(
        viewportWidth = 800f,
        viewportHeight = 600f,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
        indicatorThickness = 16f,
    )
}
