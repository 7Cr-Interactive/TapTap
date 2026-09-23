package com.app.taptap.di

import android.content.Context
import androidx.room.Room
import com.app.taptap.data.TapTapDao
import com.app.taptap.data.TapTapDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): TapTapDatabase {
        return Room.databaseBuilder(
            context,
            TapTapDatabase::class.java,
            "taptap_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideDao(database: TapTapDatabase): TapTapDao {
        return database.dao()
    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): com.google.firebase.analytics.FirebaseAnalytics {
        return com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
    }
}
