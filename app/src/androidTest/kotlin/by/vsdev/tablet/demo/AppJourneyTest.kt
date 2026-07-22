package by.vsdev.tablet.demo

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun validDimensionsBuildAndOpenTable() {
        composeRule.onNode(hasSetTextAction() and hasText("Rows")).performTextInput("2")
        composeRule.onNode(hasSetTextAction() and hasText("Columns")).performTextInput("2")
        composeRule.onNodeWithText("Build table").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Table · 2 × 2").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Table · 2 × 2").assertExists()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithContentDescription("Row 1, column 1:", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
