package com.dicoding.asclepius.presentation.uievent

import android.net.Uri
import com.dicoding.asclepius.domain.common.StringRes
import com.dicoding.asclepius.domain.model.ModelOutput

sealed class PredictionUIEvent {
    data class OnClassificationFailed(
        val message: StringRes
    ) : PredictionUIEvent()

    data class OnClassificationSuccess(
        val modelOutput: ModelOutput,
        val imageUri: Uri
    ) : PredictionUIEvent()

    data class OnPickingPreCroppedImageSuccessfully(
        val imageUriPath: String
    ) : PredictionUIEvent()

    data object FailedStartingAnalyzingProcess : PredictionUIEvent()
}