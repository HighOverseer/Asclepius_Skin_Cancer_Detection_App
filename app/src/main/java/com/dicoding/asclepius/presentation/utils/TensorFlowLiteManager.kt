package com.dicoding.asclepius.presentation.utils

import android.content.Context
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tensorflow.lite.task.gms.vision.TfLiteVision
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TensorFlowLiteManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isTFLiteVisionSuccessfullyInitialized = MutableStateFlow<Boolean?>(null)
    val isTFLiteVisionSuccessfullyInitialized = _isTFLiteVisionSuccessfullyInitialized.asStateFlow()

    init {
        initTFLiteVision()
    }

    fun initTFLiteVision() {
        _isTFLiteVisionSuccessfullyInitialized.value = null

        TfLiteGpu.isGpuDelegateAvailable(context).onSuccessTask { gpuAvailable ->
            val optionsBuilder = TfLiteInitializationOptions.builder()

            if (gpuAvailable) {
                optionsBuilder.setEnableGpuDelegateSupport(true)
            }
            TfLiteVision.initialize(context, optionsBuilder.build())
        }.addOnSuccessListener {
            _isTFLiteVisionSuccessfullyInitialized.value = true
        }.addOnFailureListener {
            _isTFLiteVisionSuccessfullyInitialized.value = false
        }
    }
}