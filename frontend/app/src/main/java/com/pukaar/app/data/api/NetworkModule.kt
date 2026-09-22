package com.pukaar.app.data.api

import com.pukaar.app.BuildConfig
import com.pukaar.app.data.local.SessionStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object NetworkModule {
    /**
     * Cached token to avoid runBlocking in OkHttp interceptor (causes ANR / "app keeps stopping").
     * Updated via Flow collector when SessionStore changes.
     */
    private val cachedToken = AtomicReference<String?>(null)
    private var tokenCollectorStarted = false

    fun api(sessionStore: SessionStore): PukaarApi {
        // Start background collector once — keeps cachedToken in sync with DataStore.
        if (!tokenCollectorStarted) {
            tokenCollectorStarted = true
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                sessionStore.accessToken.collectLatest { token ->
                    cachedToken.set(token)
                }
            }
        }

        val auth = Interceptor { chain ->
            val token = cachedToken.get()
            val req = if (token.isNullOrBlank()) chain.request() else chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(req)
        }
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(auth)
            .addInterceptor(logging)
            .build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PukaarApi::class.java)
    }
}
