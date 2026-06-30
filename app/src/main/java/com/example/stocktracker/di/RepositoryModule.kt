package com.example.stocktracker.di

import com.example.stocktracker.data.repository.BinanceCryptoRepositoryImpl
import com.example.stocktracker.data.repository.BinancePopularCryptoRepositoryImpl
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.domain.repository.PopularCryptoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCryptoRepository(impl: BinanceCryptoRepositoryImpl): CryptoRepository

    @Binds
    @Singleton
    abstract fun bindPopularCryptoRepository(
        impl: BinancePopularCryptoRepositoryImpl,
    ): PopularCryptoRepository
}
