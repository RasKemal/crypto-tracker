package com.example.stocktracker.di

import android.content.Context
import androidx.room.Room
import com.example.stocktracker.data.local.MidasDatabase
import com.example.stocktracker.data.local.dao.CryptoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMidasDatabase(
        @ApplicationContext context: Context,
    ): MidasDatabase = Room.databaseBuilder(
        context,
        MidasDatabase::class.java,
        MidasDatabase.DATABASE_NAME,
    ).build()

    @Provides
    @Singleton
    fun provideCryptoDao(database: MidasDatabase): CryptoDao =
        database.cryptoDao()
}
