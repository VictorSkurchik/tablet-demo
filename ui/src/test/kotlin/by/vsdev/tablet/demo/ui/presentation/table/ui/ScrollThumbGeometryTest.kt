package by.vsdev.tablet.demo.ui.presentation.table.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScrollThumbGeometryTest {
    @Test
    fun `returns no thumb when content fits in viewport`() {
        assertNull(
            calculateScrollThumbGeometry(
                trackLength = 500f,
                viewportLength = 600f,
                contentLength = 600f,
                scrollOffset = 0f,
                minimumThumbLength = 32f,
            ),
        )
    }

    @Test
    fun `thumb starts at top and reaches bottom`() {
        val start = geometry(scrollOffset = 0f)
        val end = geometry(scrollOffset = CONTENT_HEIGHT - VIEWPORT_LENGTH)

        assertEquals(0f, start.offset)
        assertEquals(TRACK_LENGTH - end.length, end.offset)
    }

    @Test
    fun `thumb height remains constant while scrolling`() {
        val start = geometry(scrollOffset = 0f)
        val middle = geometry(scrollOffset = 1_500f)
        val end = geometry(scrollOffset = CONTENT_HEIGHT - VIEWPORT_LENGTH)

        assertEquals(start.length, middle.length)
        assertEquals(start.length, end.length)
    }

    @Test
    fun `thumb position represents the scroll fraction`() {
        val middle = geometry(scrollOffset = (CONTENT_HEIGHT - VIEWPORT_LENGTH) / 2f)
        val expectedTop = (TRACK_LENGTH - middle.length) / 2f

        assertEquals(expectedTop, middle.offset, 0.001f)
    }

    @Test
    fun `thumb respects minimum height and clamps scroll offset`() {
        val beforeStart = geometry(scrollOffset = -100f, contentHeight = 100_000f)
        val afterEnd = geometry(scrollOffset = 200_000f, contentHeight = 100_000f)

        assertEquals(MINIMUM_THUMB_LENGTH, beforeStart.length)
        assertEquals(0f, beforeStart.offset)
        assertEquals(TRACK_LENGTH - afterEnd.length, afterEnd.offset)
    }

    private fun geometry(
        scrollOffset: Float,
        contentHeight: Float = CONTENT_HEIGHT,
    ): ScrollThumbGeometry =
        checkNotNull(
            calculateScrollThumbGeometry(
                trackLength = TRACK_LENGTH,
                viewportLength = VIEWPORT_LENGTH,
                contentLength = contentHeight,
                scrollOffset = scrollOffset,
                minimumThumbLength = MINIMUM_THUMB_LENGTH,
            ),
        )

    private companion object {
        const val TRACK_LENGTH = 500f
        const val VIEWPORT_LENGTH = 1_000f
        const val CONTENT_HEIGHT = 5_000f
        const val MINIMUM_THUMB_LENGTH = 32f
    }
}
