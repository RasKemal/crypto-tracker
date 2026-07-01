package com.example.stocktracker.di

import android.content.Context
import androidx.room.Room
import com.example.stocktracker.data.local.CryptoDatabase
import com.example.stocktracker.data.local.dao.WatchlistDao
import com.example.stocktracker.data.local.dao.MarketAssetDao
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
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    @Provides
    @Singleton
    fun provideWatchlistDao(database: CryptoDatabase): WatchlistDao =
        database.watchlistDao()

    @Provides
    @Singleton
    fun provideMarketAssetDao(database: CryptoDatabase): MarketAssetDao =
        database.marketAssetDao()
}
