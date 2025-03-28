package com.dicoding.asclepius.presentation.utils.image

import android.net.Uri
import javax.inject.Inject

const val AUTHORITY = "com.dicoding.asclepius.fileprovider"

class ImageCaptureHandler @Inject constructor(
    private val fileManager: FileManager
) {
    var latestImageCapturedUri: Uri? = null


    fun provideCapturedImageUri(): Uri {
        val uri = fileManager.getContentUriWithFileProvider()
        latestImageCapturedUri = uri

        return uri
    }

    fun clearLatestCapturedImageUri() {
        latestImageCapturedUri?.let {
            fileManager.deleteContentUriFromFileProvider(it)
        }
        latestImageCapturedUri = null
    }

}