package com.dicoding.asclepius.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelOutput(
    val label:String,
    val confidenceScore:Float
):Parcelable