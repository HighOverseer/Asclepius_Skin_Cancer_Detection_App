package com.dicoding.asclepius.di

import com.dicoding.asclepius.data.RepositoryImpl
import com.dicoding.asclepius.domain.data.Repository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    abstract fun bindRepository(repositoryImpl: RepositoryImpl): Repository


}