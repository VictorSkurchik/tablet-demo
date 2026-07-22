package by.vsdev.tablet.demo.ui.presentation.table.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScrollThumbGeometryTest {
    @Test
    fun `returns no thumb when content fits in viewport`() {
        assertNull(
            calculateScrollThumbGeometry(
                trackHeight = 500f,
                viewportHeight = 600f,
                contentHeight = 600f,
                scrollOffset = 0f,
                minimumThumbHeight = 32f,
            ),
        )
    }

    @Test
    fun `thumb starts at top and reaches bottom`() {
        val start = geometry(scrollOffset = 0f)
        val end = geometry(scrollOffset = CONTENT_HEIGHT - VIEWPORT_HEIGHT)

        assertEquals(0f, start.top)
        assertEquals(TRACK_HEIGHT - end.height, end.top)
    }

    @Test
    fun `thumb height remains constant while scrolling`() {
        val start = geometry(scrollOffset = 0f)
        val middle = geometry(scrollOffset = 1_500f)
        val end = geometry(scrollOffset = CONTENT_HEIGHT - VIEWPORT_HEIGHT)

        assertEquals(start.height, middle.height)
        assertEquals(start.height, end.height)
    }

    @Test
    fun `thumb position represents the scroll fraction`() {
        val middle = geometry(scrollOffset = (CONTENT_HEIGHT - VIEWPORT_HEIGHT) / 2f)
        val expectedTop = (TRACK_HEIGHT - middle.height) / 2f

        assertEquals(expectedTop, middle.top, 0.001f)
    }

    @Test
    fun `thumb respects minimum height and clamps scroll offset`() {
        val beforeStart = geometry(scrollOffset = -100f, contentHeight = 100_000f)
        val afterEnd = geometry(scrollOffset = 200_000f, contentHeight = 100_000f)

        assertEquals(MINIMUM_THUMB_HEIGHT, beforeStart.height)
        assertEquals(0f, beforeStart.top)
        assertEquals(TRACK_HEIGHT - afterEnd.height, afterEnd.top)
    }

    private fun geometry(
        scrollOffset: Float,
        contentHeight: Float = CONTENT_HEIGHT,
    ): ScrollThumbGeometry =
        checkNotNull(
            calculateScrollThumbGeometry(
                trackHeight = TRACK_HEIGHT,
                viewportHeight = VIEWPORT_HEIGHT,
                contentHeight = contentHeight,
                scrollOffset = scrollOffset,
                minimumThumbHeight = MINIMUM_THUMB_HEIGHT,
            ),
        )

    private companion object {
        const val TRACK_HEIGHT = 500f
        const val VIEWPORT_HEIGHT = 1_000f
        const val CONTENT_HEIGHT = 5_000f
        const val MINIMUM_THUMB_HEIGHT = 32f
    }
}
