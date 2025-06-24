package com.me.gemini.di

import android.content.Context
import com.me.gemini.data.GeminiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideGeminiRepository(
        @ApplicationContext context: Context
    ): GeminiRepository {
        return GeminiRepository(context)
    }
}