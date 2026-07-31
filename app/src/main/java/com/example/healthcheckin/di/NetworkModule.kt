package com.example.healthcheckin.di

import android.content.Context
import com.example.healthcheckin.BuildConfig
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.remote.AccountApi
import com.example.healthcheckin.data.remote.AuthApi
import com.example.healthcheckin.data.remote.FoodSearchApi
import com.example.healthcheckin.data.remote.SupabaseSyncApi
import com.example.healthcheckin.data.remote.AuthInterceptor
import com.example.healthcheckin.data.remote.ServerTimeInterceptor
import com.example.healthcheckin.data.remote.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideJson(): Json = json

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionManager: SessionManager,
        tokenAuthenticator: TokenAuthenticator,
        serverTimeInterceptor: ServerTimeInterceptor,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionManager))
            .addNetworkInterceptor(serverTimeInterceptor)
            .authenticator(tokenAuthenticator)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                    redactHeader("Authorization")
                    redactHeader("apikey")
                }
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("${BuildConfig.SUPABASE_URL}/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideFoodSearchApi(retrofit: Retrofit): FoodSearchApi =
        retrofit.create(FoodSearchApi::class.java)

    @Provides
    @Singleton
    fun provideSupabaseSyncApi(retrofit: Retrofit): SupabaseSyncApi =
        retrofit.create(SupabaseSyncApi::class.java)

    @Provides
    @Singleton
    fun provideAccountApi(retrofit: Retrofit): AccountApi =
        retrofit.create(AccountApi::class.java)

    @Provides
    @Singleton
    fun provideDeviceId(@ApplicationContext context: Context): String {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        return prefs.getString("device_id", null)
            ?: com.example.healthcheckin.util.UuidV7.generateDeviceId().also { id ->
                prefs.edit().putString("device_id", id).apply()
            }
    }
}
