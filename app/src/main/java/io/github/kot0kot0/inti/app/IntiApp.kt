package io.github.kot0kot0.inti.app

import android.app.Application
import io.github.kot0kot0.inti.app.BuildConfig
import timber.log.Timber

class IntiApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Syncが成功していれば Timber と BuildConfig が認識されます
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}