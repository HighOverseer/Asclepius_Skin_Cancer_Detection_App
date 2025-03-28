package com.dicoding.asclepius.presentation.utils.image

sealed class PickImageMediaResult {
    data object CameraSuccess : PickImageMediaResult()
    data class GallerySuccess(val imageUriPath: String) : PickImageMediaResult()
    data object Failed : PickImageMediaResult()
}