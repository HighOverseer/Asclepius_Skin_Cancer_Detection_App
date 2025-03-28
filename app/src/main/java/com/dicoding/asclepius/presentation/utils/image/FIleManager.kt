package com.dicoding.asclepius.presentation.utils.image

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getContentUriWithFileProvider(fileName: String = getFileName()): Uri {
        val imageFile = getFile(fileName)

        return FileProvider.getUriForFile(
            context,
            AUTHORITY,
            imageFile
        )
    }

    fun getFile(fileName: String = getFileName()): File {
        val fileDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(fileDir, "/$fileName")
        if (imageFile.parentFile?.exists() == false) imageFile.parentFile?.mkdirs()

        if (!imageFile.exists()) imageFile.createNewFile()

        return imageFile
    }

    fun deleteContentUriFromFileProvider(uri: Uri) {
        val file =
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                uri.lastPathSegment ?: ""
            )
        if (file.exists()) file.delete()
    }

    fun deleteFromFilePath(filePath: String) {
        val file = File(filePath)
        if (file.exists()) file.delete()
    }

    private fun getFileName(): String {
        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
        return "${timeStamp.format(Date())}_${System.currentTimeMillis()}.jpg"
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
    }

}