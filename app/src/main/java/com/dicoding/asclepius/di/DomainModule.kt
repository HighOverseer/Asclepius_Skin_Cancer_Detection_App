package com.dicoding.asclepius.di

import com.dicoding.asclepius.data.mapper.MapperDtoToModel
import com.dicoding.asclepius.data.mapper.MapperModelToDto
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class DomainModule {

    @Provides
    fun provideMapperDtoToModel() = MapperDtoToModel

    @Provides
    fun provideMapperModelToDto() = MapperModelToDto
}