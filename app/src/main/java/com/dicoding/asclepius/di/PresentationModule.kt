package com.dicoding.asclepius.di


import com.dicoding.asclepius.domain.presentation.ImageClassifierHelper
import com.dicoding.asclepius.presentation.utils.ml.ImageClassifierHelperImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PresentationModule {

    @Binds
    @Singleton
    abstract fun bindImageClassifierHelper(imageClassifierHelperImpl: ImageClassifierHelperImpl): ImageClassifierHelper


}