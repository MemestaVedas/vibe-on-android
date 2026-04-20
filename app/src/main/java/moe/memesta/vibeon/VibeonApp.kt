package moe.memesta.vibeon

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import moe.memesta.vibeon.di.appContainerModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

@HiltAndroidApp
class VibeonApp : Application() {

    lateinit var container: AppContainer

    companion object {
        lateinit var instance: VibeonApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)

        startKoin {
            androidContext(this@VibeonApp)
            modules(appContainerModule(container))
        }
    }
}
