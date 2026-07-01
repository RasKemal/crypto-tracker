package com.example.stocktracker.core.di

import android.content.Context
import androidx.room.Room
import com.example.stocktracker.data.local.CryptoDatabase
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
    fun provideCryptoDatabase(
        @ApplicationContext context: Context,
    ): CryptoDatabase = Room.databaseBuilder(
        context,
        CryptoDatabase::class.java,
        CryptoDatabase.DATABASE_NAME,
    ).build()

    @Provides
    @Singleton
    fun provideCryptoDao(database: CryptoDatabase): CryptoDao =
        database.cryptoDao()
}
