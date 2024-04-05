package ru.netology.nework.application

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import ru.netology.nework.BuildConfig
import ru.netology.nework.auth.AppAuth

class NeWorkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppAuth.initApp(this)
        MapKitFactory.setApiKey(BuildConfig.MAP_KEY)
    }
}