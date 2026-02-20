package moe.memesta.vibeon

import android.app.Application

class VibeonApp : Application() {

    companion object {
        lateinit var instance: VibeonApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
