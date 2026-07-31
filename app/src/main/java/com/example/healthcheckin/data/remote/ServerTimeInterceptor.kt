package com.example.healthcheckin.data.remote

import com.example.healthcheckin.util.DeviceTimeMonitor
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class ServerTimeInterceptor @Inject constructor(
    private val deviceTimeMonitor: DeviceTimeMonitor,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        response.header("Date")?.let { deviceTimeMonitor.recordServerTimeFromHttpDate(it) }
        return response
    }
}
