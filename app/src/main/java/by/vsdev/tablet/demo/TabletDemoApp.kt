package by.vsdev.tablet.demo

import android.app.Application
import by.vsdev.tablet.demo.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TabletDemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TabletDemoApp)
            modules(appModules)
        }
    }
}
