package com.example.stocktracker.di

import com.example.stocktracker.data.repository.BinanceCryptoRepositoryImpl
import com.example.stocktracker.domain.repository.CryptoRepository
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
}
