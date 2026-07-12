package com.laundry.stockapp.di

import android.content.Context
import com.laundry.stockapp.data.repository.FirestoreRepository
import com.laundry.stockapp.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideFirestoreRepository(): FirestoreRepository {
        return FirestoreRepository()
    }
}
