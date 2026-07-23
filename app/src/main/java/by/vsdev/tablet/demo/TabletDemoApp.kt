package by.vsdev.tablet.demo

import android.app.Application
import by.vsdev.tablet.demo.di.appModules
import by.vsdev.tablet.demo.recovery.TableRecoveryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TabletDemoApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val koinApplication =
            startKoin {
                androidContext(this@TabletDemoApp)
                modules(appModules)
            }
        applicationScope.launch {
            koinApplication.koin.get<TableRecoveryRepository>().cleanupExpired()
        }
    }
}
