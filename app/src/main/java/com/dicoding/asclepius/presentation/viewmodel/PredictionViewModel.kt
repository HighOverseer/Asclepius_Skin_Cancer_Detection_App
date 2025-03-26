package com.dicoding.asclepius.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.distinctUntilChanged
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
    savedStateHandle: SavedStateHandle,
) : ViewModel(), ClassifierListener {

    private val _uiState = MutableStateFlow(
        PredictionUIState(
            isInClassifyingProcess = savedStateHandle[IS_IN_CLASSIFYING_PROCESS] ?: false,
            isReadyToShowLatestImage = savedStateHandle[IS_READY_TO_SHOW_LATEST_IMAGE] ?: false,
            isStillInitializingTFLiteVision = savedStateHandle[IS_STILL_INITIALIZING_TFLITE_VISION] ?: false,
            currentImageUriPath = savedStateHandle[CURRENT_IMAGE_URI_PATH]
        )
    )

    val uiState = _uiState.asStateFlow()

    private val _uiEvent = Channel<PredictionUIEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    var latestCroppedImageFilePath: String? = savedStateHandle[LATEST_CROPPED_IMAGE_PATH]

    init {
        imageCaptureHandler.latestImageCapturedUri = savedStateHandle.get<String?>(LATEST_CAPTURED_IMAGE_URI_PATH)?.let {
            Uri.parse(it)
        }

        imageClassifierHelper.setClassificationListener(this)

        viewModelScope.launch {
            observeTfLiteInitializationStatus()
            observeUIStateToSaveInSaveStateHandle(savedStateHandle)
        }
    }

    private suspend fun observeTfLiteInitializationStatus() {
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

    private suspend fun observeUIStateToSaveInSaveStateHandle(
        savedStateHandle:SavedStateHandle
    ){
        _uiState.distinctUntilChanged { old, new ->  old == new }.collectLatest {
            savedStateHandle[IS_READY_TO_SHOW_LATEST_IMAGE] = it.isReadyToShowLatestImage
            savedStateHandle[IS_IN_CLASSIFYING_PROCESS] = it.isInClassifyingProcess
            savedStateHandle[IS_STILL_INITIALIZING_TFLITE_VISION] = it.isStillInitializingTFLiteVision
            savedStateHandle[CURRENT_IMAGE_URI_PATH] = it.currentImageUriPath
            savedStateHandle[LATEST_CAPTURED_IMAGE_URI_PATH] = imageCaptureHandler.latestImageCapturedUri.toString()
            savedStateHandle[LATEST_CROPPED_IMAGE_PATH] = latestCroppedImageFilePath
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

    companion object{
        private const val IS_IN_CLASSIFYING_PROCESS = "is_in_classifying_process"
        private const val CURRENT_IMAGE_URI_PATH = "current_image_uri_path"
        private const val IS_READY_TO_SHOW_LATEST_IMAGE = "is_ready_to_show_latest_image"
        private const val IS_STILL_INITIALIZING_TFLITE_VISION = "is_still_initializing_tflite_vision"
        private const val LATEST_CROPPED_IMAGE_PATH = "latest_cropped_image_path"
        private const val LATEST_CAPTURED_IMAGE_URI_PATH = "latest_captured_image_uri_path"
    }
}