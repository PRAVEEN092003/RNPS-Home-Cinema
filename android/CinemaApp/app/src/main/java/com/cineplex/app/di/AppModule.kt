package com.cineplex.app.di

import android.content.Context
import com.cineplex.app.BuildConfig
import com.cineplex.app.data.api.CinemaApiService
import com.cineplex.app.data.api.RetrofitFactory
import com.cineplex.app.util.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiService(
        @ApplicationContext context: Context,
        sessionManager: SessionManager,
    ): CinemaApiService {
        return RetrofitFactory.create(
            baseUrl = BuildConfig.API_BASE_URL,
            tokenProvider = { sessionManager.getTokenSync() },
        )
    }
}
