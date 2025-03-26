package com.dicoding.asclepius.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.asclepius.R
import com.dicoding.asclepius.domain.common.StringRes
import com.dicoding.asclepius.domain.model.ModelOutput
import com.dicoding.asclepius.domain.presentation.ClassifierListener
import com.dicoding.asclepius.domain.presentation.ImageClassifierHelper
import com.dicoding.asclepius.presentation.uievent.PredictionUIEvent
import com.dicoding.asclepius.presentation.uistate.PredictionUIState
import com.dicoding.asclepius.presentation.utils.CroppedImageResult
import com.dicoding.asclepius.presentation.utils.ImageCaptureHandler
import com.dicoding.asclepius.presentation.utils.PickImageMediaResult
import com.dicoding.asclepius.presentation.utils.TensorFlowLiteManager
import com.dicoding.asclepius.presentation.utils.deleteFromFileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PredictionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCaptureHandler: ImageCaptureHandler,
    private val imageClassifierHelper: ImageClassifierHelper,
    private val tensorFlowLiteManager: TensorFlowLiteManager,
) : ViewModel(), ClassifierListener {


    private val _uiState = MutableStateFlow(PredictionUIState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = Channel<PredictionUIEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    var latestCroppedImageFilePath: String? = null

    init {
        imageClassifierHelper.setClassificationListener(this)
        viewModelScope.launch {
            tensorFlowLiteManager.isTFLiteVisionSuccessfullyInitialized.collectLatest {
                val isInitializing = it == null
                _uiState.update { state ->
                    state.copy(isStillInitializingTFLiteVision = isInitializing)
                }

                if (it == false) {
                    tensorFlowLiteManager.initTFLiteVision()
                }
            }
        }
    }

    fun analyzeImage() {
        if (_uiState.value.isInClassifyingProcess || _uiState.value.isStillInitializingTFLiteVision) {
            viewModelScope.launch {
                _uiEvent.send(PredictionUIEvent.FailedStartingAnalyzingProcess)
            }
            return
        }

        _uiState.value.currentImageUriPath?.let { imageUriPath ->
            _uiState.update { it.copy(isInClassifyingProcess = true) }
            imageClassifierHelper.classifyStaticImage(imageUriPath)
            return
        }

        viewModelScope.launch {
            _uiEvent.send(
                PredictionUIEvent.OnClassificationFailed(
                    StringRes.Static(
                        R.string.please_select_an_image_first
                    )
                )
            )
        }
    }

    fun handleOnResultCroppedImageSession(result: CroppedImageResult) {

        when (result) {
            CroppedImageResult.Canceled -> {
                _uiState.update {
                    it.copy(currentImageUriPath = null)
                }
                latestCroppedImageFilePath?.let {
                    val canceledCroppedImageFile = File(it)
                    if (canceledCroppedImageFile.exists()) canceledCroppedImageFile.delete()
                }
                latestCroppedImageFilePath = null
            }

            is CroppedImageResult.Success -> {
                _uiState.update {
                    it.copy(
                        currentImageUriPath = result.imageUriPath
                    )
                }
            }

            CroppedImageResult.Failed -> {
                _uiState.update {
                    it.copy(
                        currentImageUriPath = null
                    )
                }
            }
        }

        imageCaptureHandler.clearLatestCapturedImageUri(context)
        _uiState.update { it.copy(isReadyToShowLatestImage = true) }
    }

    fun handleOnPickImageMediaResult(result: PickImageMediaResult) {
        when (result) {
            is PickImageMediaResult.GallerySuccess -> {
                _uiState.update {
                    it.copy(
                        currentImageUriPath = result.imageUriPath,
                        isReadyToShowLatestImage = false
                    )
                }
                viewModelScope.launch {
                    _uiEvent.send(
                        PredictionUIEvent.OnPickingPreCroppedImageSuccessfully(result.imageUriPath)
                    )
                }
            }

            is PickImageMediaResult.CameraSuccess -> {
                imageCaptureHandler.latestImageCapturedUri?.let { uri ->
                    _uiState.update {
                        it.copy(
                            currentImageUriPath = uri.toString(),
                            isReadyToShowLatestImage = false
                        )
                    }
                    viewModelScope.launch {
                        _uiEvent.send(
                            PredictionUIEvent.OnPickingPreCroppedImageSuccessfully(uri.toString())
                        )
                    }
                }
            }

            PickImageMediaResult.Failed -> {
                _uiState.update {
                    it.copy(
                        currentImageUriPath = null,
                        isReadyToShowLatestImage = true
                    )
                }
                imageCaptureHandler.clearLatestCapturedImageUri(context)
            }
        }
    }

    fun resetSession() {
        _uiState.update { it.copy(currentImageUriPath = null) }
    }

    override fun onResult(imageUri: Uri, outputs: List<ModelOutput>?, inferenceTime: Long) {
        _uiState.update { it.copy(isInClassifyingProcess = false) }

        viewModelScope.launch {
            val output = outputs?.first() ?: return@launch
            _uiEvent.send(
                PredictionUIEvent.OnClassificationSuccess(
                    modelOutput = output,
                    imageUri = imageUri
                )
            )
        }
    }

    fun prepareForPickingUpAnImageFromMedia() {
        val currentImagePath = _uiState.value.currentImageUriPath

        _uiState.update {
            it.copy(currentImageUriPath = null)
        }
        currentImagePath?.let {
            Uri.parse(it)?.apply {
                deleteFromFileProvider(context, this)
            }
        }
        _uiState.update {
            it.copy(isReadyToShowLatestImage = false)
        }
    }

    fun provideCapturedImageUriForNewSession(): Uri {
        return imageCaptureHandler.provideCapturedImageUri(context)
    }

    override fun onError(message: StringRes) {
        _uiState.update { it.copy(isInClassifyingProcess = false) }

        viewModelScope.launch {
            _uiEvent.send(
                PredictionUIEvent.OnClassificationFailed(message)
            )
        }
    }

    fun clearingAllUnusedImageUris() {
        _uiState.update {
            it.currentImageUriPath?.let { uriPath ->
                Uri.parse(uriPath)?.apply {
                    deleteFromFileProvider(context, this)
                }
            }
            it.copy(currentImageUriPath = null)
        }
        latestCroppedImageFilePath?.let {
            val canceledCroppedImageFile = File(it)
            if (canceledCroppedImageFile.exists()) canceledCroppedImageFile.delete()
        }
        latestCroppedImageFilePath = null
        imageCaptureHandler.clearLatestCapturedImageUri(context)
    }

}