package com.dicoding.asclepius.domain.presentation

import android.net.Uri
import com.dicoding.asclepius.domain.common.StringRes
import com.dicoding.asclepius.domain.model.ModelOutput

interface ClassifierListener {

    fun onResult(
        imageUri: Uri,
        outputs: List<ModelOutput>?,
        inferenceTime: Long
    )

    fun onError(message: StringRes)
}