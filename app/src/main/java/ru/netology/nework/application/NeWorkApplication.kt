package ru.netology.nework.application

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp
import ru.netology.nework.BuildConfig
import ru.netology.nework.auth.AppAuth
import javax.inject.Inject

@HiltAndroidApp
class NeWorkApplication : Application() {
    @Inject
    lateinit var auth: AppAuth
    override fun onCreate() {
        super.onCreate()
        //AppAuth.initApp(this)
        MapKitFactory.setApiKey(BuildConfig.MAP_KEY)
    }
}