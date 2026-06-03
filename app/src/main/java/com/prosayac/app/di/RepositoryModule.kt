package com.prosayac.app.di

import com.prosayac.app.data.repository.MeterRepositoryImpl
import com.prosayac.app.domain.repository.MeterRepository
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
    abstract fun bindMeterRepository(
        impl: MeterRepositoryImpl
    ): MeterRepository
}