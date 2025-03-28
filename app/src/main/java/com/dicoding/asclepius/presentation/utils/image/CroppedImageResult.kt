package com.dicoding.asclepius.presentation.utils.image

sealed class CroppedImageResult {
    data object Canceled : CroppedImageResult()
    data class Success(val imageUriPath: String) : CroppedImageResult()
    data object Failed : CroppedImageResult()
}