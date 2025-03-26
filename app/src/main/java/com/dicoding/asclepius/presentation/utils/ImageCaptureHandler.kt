package com.dicoding.asclepius.presentation.utils

import android.content.Context
import android.net.Uri
import javax.inject.Inject

const val AUTHORITY = "com.dicoding.asclepius.fileprovider"

class ImageCaptureHandler @Inject constructor() {
    var latestImageCapturedUri: Uri? = null


    fun provideCapturedImageUri(context: Context): Uri {
        val uri: Uri

        val fileName = getFileName()

        uri = getContentUriWithFileProvider(context, fileName)

        latestImageCapturedUri = uri
        return uri
    }

    fun clearLatestCapturedImageUri(context: Context) {
        latestImageCapturedUri?.let {
            deleteFromFileProvider(context, it)
        }
        latestImageCapturedUri = null
    }


}