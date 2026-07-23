package by.vsdev.tablet.demo.ui.presentation.table.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorPaneHeightTest {
    @Test
    fun visibleImeUsesHalfOfAvailableHeight() {
        val height =
            calculateEditorPaneTargetHeight(
                shouldExpand = true,
                maxHeight = 1200.dp,
                defaultHeight = 420.dp,
            )

        assertEquals(600.dp, height)
    }

    @Test
    fun hiddenImeUsesDirectiveDefaultHeight() {
        val height =
            calculateEditorPaneTargetHeight(
                shouldExpand = false,
                maxHeight = 1200.dp,
                defaultHeight = 420.dp,
            )

        assertEquals(420.dp, height)
    }
}
