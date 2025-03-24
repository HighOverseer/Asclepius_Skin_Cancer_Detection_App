package com.dicoding.asclepius.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.dicoding.asclepius.domain.common.StringRes
import com.dicoding.asclepius.domain.utils.DomainConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Suppress("DEPRECATION")
fun Context.convertImageUriToBitmap(imageUri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(contentResolver, imageUri)
        ImageDecoder.decodeBitmap(source)
    } else {
        MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
    }.copy(Bitmap.Config.ARGB_8888, true)
}

fun ImageView.loadImage(bitmap: Bitmap?) {
    Glide.with(context)
        .load(bitmap)
        .into(this)
}

fun ImageView.loadImage(imageUri: Uri) {
    Glide.with(context)
        .load(imageUri)
        .into(this)
}

fun ImageView.loadImage(imageUrl: String) {
    Glide.with(context)
        .load(imageUrl)
        .into(this)
}

fun ImageView.cancelRequest() {
    Glide.with(context).clear(this)
}

private const val MAX_IMAGE_SIZE_KB = 512 * 1000
suspend fun Context.convertImageUriToReducedBitmap(imageUri: Uri): Bitmap =
    withContext(Dispatchers.Default) {

        val bitmap = convertImageUriToBitmap(imageUri)

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
        } while (streamLength > MAX_IMAGE_SIZE_KB && compressQuality > 0)

        val compressedByteArray = bmpStream.toByteArray()

        return@withContext BitmapFactory.decodeByteArray(
            compressedByteArray,
            0,
            compressedByteArray.size
        )

    }

fun Float.formatToPercentage(): String {
    return NumberFormat.getPercentInstance().format(this)
}

fun AppCompatActivity.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Fragment.showToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun getCurrentDateToString(): String {
    val calendar = Calendar.getInstance()
    val df = SimpleDateFormat(DomainConstants.DATE_STRING_FORMAT, Locale.getDefault())
    return df.format(calendar.time)
}

fun <T> LifecycleOwner.collectLatestOnLifeCycleStarted(
    stateFlow: StateFlow<T>, onCollectLatest: suspend (T) -> Unit
) {
    this.lifecycleScope.launch {
        this@collectLatestOnLifeCycleStarted.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            stateFlow.collectLatest(action = onCollectLatest)
        }
    }
}

fun <T> LifecycleOwner.collectChannelFlowWhenStarted(
    channelFlow: Flow<T>, onCollect: suspend (T) -> Unit
) {
    this.lifecycleScope.launch {
        this@collectChannelFlowWhenStarted.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                channelFlow.collect(onCollect)
            }
        }
    }
}


fun StringRes.getValue(context: Context): String {
    return when (this) {
        is StringRes.Static -> {
            context.getString(resId, args)
        }

        is StringRes.Dynamic -> {
            value
        }
    }
}

private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"

fun getFileName(): String {
    val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
    return "${timeStamp.format(Date())}_${System.currentTimeMillis()}.jpg"
}

fun getContentUriWithFileProvider(context: Context, fileName: String): Uri {
    val imageFile = getFile(context, fileName)

    return FileProvider.getUriForFile(
        context,
        AUTHORITY,
        imageFile
    )
}

fun getFile(context: Context, fileName: String): File {
    val fileDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File(fileDir, "/$fileName")
    if (imageFile.parentFile?.exists() == false) imageFile.parentFile?.mkdirs()

    if (!imageFile.exists()) imageFile.createNewFile()

    return imageFile
}

fun deleteFromFileProvider(context: Context, uri: Uri) {
    val file =
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), uri.lastPathSegment ?: "")
    if (file.exists()) file.delete()
}

fun Context.getColorFromAttr(attr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}