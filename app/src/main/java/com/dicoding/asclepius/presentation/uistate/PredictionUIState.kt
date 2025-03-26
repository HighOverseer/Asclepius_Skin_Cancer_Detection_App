package com.dicoding.asclepius.presentation.uistate

data class PredictionUIState(
    val isInClassifyingProcess: Boolean = false,
    val currentImageUriPath: String? = null,
    val isStillInitializingTFLiteVision: Boolean = false,
    val isReadyToShowLatestImage: Boolean = false
)