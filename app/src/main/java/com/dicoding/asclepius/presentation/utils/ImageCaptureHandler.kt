package com.dicoding.asclepius.presentation.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val AUTHORITY = "com.dicoding.asclepius.fileprovider"

class ImageCaptureHandler(
    timeStampPattern:String = FILENAME_FORMAT
) {
    lateinit var latestImageCaptured: Uri

    private val timeStamp = SimpleDateFormat(timeStampPattern, Locale.getDefault())

    fun getImageUri(context: Context):Uri{
        var uri:Uri? = null

        val fileName = getFileName()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyCamera/")
            }

            uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        }

        if (uri != null){
            latestImageCaptured = uri
            return uri
        }

        val fileDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(fileDir, "/MyCamera/$fileName")
        if(imageFile.parentFile?.exists() == false) imageFile.parentFile?.mkdir()

        return FileProvider.getUriForFile(
            context,
            AUTHORITY,
            imageFile
        ).also {
            latestImageCaptured = it
        }
    }

    private fun getFileName():String{
        return "${timeStamp.format(Date())}.jpg"
    }


    companion object{
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
    }
}