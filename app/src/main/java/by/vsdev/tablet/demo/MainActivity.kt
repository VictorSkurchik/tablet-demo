package by.vsdev.tablet.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import by.vsdev.tablet.demo.navigation.AppRoot
import by.vsdev.tablet.demo.recovery.RecoverySessionIdFactory
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val recoverySessionIdFactory by inject<RecoverySessionIdFactory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                AppRoot(recoverySessionIdFactory)
            }
        }
    }
}
