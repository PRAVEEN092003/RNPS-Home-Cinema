package com.cineplex.app.data.api

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory that builds the Retrofit client.
 * The auth token is injected via [AuthInterceptor].
 */
object RetrofitFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun create(
        baseUrl: String,
        tokenProvider: () -> String?,
        cacheDir: java.io.File? = null,
    ): CinemaApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request()
            val path = request.url.encodedPath
            val builder = request.newBuilder()
            if (!path.endsWith("/auth/register") && !path.endsWith("/auth/login") && !path.endsWith("/health")) {
                val token = tokenProvider()
                if (!token.isNullOrBlank()) {
                    builder.addHeader("Authorization", "Bearer $token")
                }
            }
            chain.proceed(builder.build())
        }

        val okhttpBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .retryOnConnectionFailure(true)
            .connectTimeout(45, TimeUnit.SECONDS) // Gracefully accommodate Render Free cold starts
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (cacheDir != null) {
            okhttpBuilder.cache(okhttp3.Cache(java.io.File(cacheDir, "http_cache"), 10L * 1024 * 1024))
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okhttpBuilder.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CinemaApiService::class.java)
    }
}
