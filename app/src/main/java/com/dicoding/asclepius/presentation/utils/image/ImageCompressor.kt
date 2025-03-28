package com.dicoding.asclepius.presentation.utils.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ImageCompressor {

    suspend fun compressBitmap(
        bitmap: Bitmap,
        maxImageSizeKB: Int = DEFAULT_MAX_IMAGE_SIZE_KB
    ): Bitmap =
        withContext(Dispatchers.Default) {
            ensureActive()

            var compressQuality = 100
            var streamLength: Int
            val bmpStream = ByteArrayOutputStream()

            do {
                ensureActive()
                bmpStream.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
                val bmpPictByteArray = bmpStream.toByteArray()
                streamLength = bmpPictByteArray.size
                compressQuality -= 5
            } while (streamLength > maxImageSizeKB && compressQuality > 0)

            val compressedByteArray = bmpStream.toByteArray()

            return@withContext BitmapFactory.decodeByteArray(
                compressedByteArray,
                0,
                compressedByteArray.size
            )

        }

    companion object {
        private const val DEFAULT_MAX_IMAGE_SIZE_KB = 512 * 1000
    }
}