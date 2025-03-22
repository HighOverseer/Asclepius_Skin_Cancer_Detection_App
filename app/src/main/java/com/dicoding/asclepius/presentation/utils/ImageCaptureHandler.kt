package com.dicoding.asclepius.presentation.utils

import android.content.Context
import android.net.Uri

const val AUTHORITY = "com.dicoding.asclepius.fileprovider"

class ImageCaptureHandler {
    var latestImageCapturedUri: Uri? = null


    fun provideCapturedImageUri(context: Context): Uri {
        val uri: Uri

        val fileName = getFileName()

        uri = getUriForBelowAndroidQ(context, fileName)

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