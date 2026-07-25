package by.vsdev.tablet.demo.ui.components.layout

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import by.vsdev.tablet.demo.ui.theme.AppSpacing

@Composable
internal fun ResponsiveFormContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 480.dp,
    contentPadding: Dp = AppSpacing.large,
    verticalSpacing: Dp = AppSpacing.medium,
    applyImePadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .then(if (applyImePadding) Modifier.imePadding() else Modifier)
                .padding(contentPadding),
    ) {
        val viewportHeight = maxHeight
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = maxWidth)
                        .fillMaxWidth()
                        .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                content = content,
            )
        }
    }
}
