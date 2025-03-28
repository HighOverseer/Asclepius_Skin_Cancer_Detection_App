package com.dicoding.asclepius.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.TypedValue
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.dicoding.asclepius.domain.common.StringRes
import com.dicoding.asclepius.domain.utils.DomainConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


fun ImageView.loadImage(bitmap: Bitmap?) {
    Glide.with(context)
        .load(bitmap)
        .into(this)
}

fun ImageView.loadImage(imageUri: Uri?) {
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

fun Float.formatToPercentage(): String {
    return NumberFormat.getPercentInstance().format(this)
}

fun Context.showToast(message: String) {
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

fun Context.getColorFromAttr(attr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}