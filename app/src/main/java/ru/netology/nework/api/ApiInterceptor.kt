package ru.netology.nework.api

import okhttp3.Interceptor
import okhttp3.Response
import ru.netology.nework.BuildConfig

class ApiInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder()
                .addHeader("Api-Key", BuildConfig.API_KEY)
                .build()
        )
    }
}