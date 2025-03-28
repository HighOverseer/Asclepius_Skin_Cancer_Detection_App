package com.dicoding.asclepius.presentation.utils.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageConverter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    @Suppress("DEPRECATION")
    fun convertImageUriToBitmap(imageUri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }.copy(Bitmap.Config.ARGB_8888, true)
    }

    suspend fun convertImageUriToReducedBitmap(
        imageUri: Uri,
        imageCompressor: ImageCompressor
    ): Bitmap {
        val bitmap = convertImageUriToBitmap(imageUri)
        return imageCompressor.compressBitmap(bitmap)
    }
}