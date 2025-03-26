package com.dicoding.asclepius.domain.presentation

interface ImageClassifierHelper {

    fun classifyStaticImage(imageUriPath: String)

    fun setClassificationListener(listener: ClassifierListener)
}