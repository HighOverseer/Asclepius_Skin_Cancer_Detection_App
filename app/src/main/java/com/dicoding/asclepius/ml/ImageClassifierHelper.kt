package com.dicoding.asclepius.ml

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import com.dicoding.asclepius.R
import com.dicoding.asclepius.presentation.utils.convertImageUriToBitmap
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import org.tensorflow.lite.DataType
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.gms.vision.TfLiteVision
import org.tensorflow.lite.task.gms.vision.classifier.Classifications
import org.tensorflow.lite.task.gms.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.gms.vision.classifier.ImageClassifier.ImageClassifierOptions


class ImageClassifierHelper(
    private val threshold:Float = ModelConstants.THRESHOLD,
    private val maxResults:Int = ModelConstants.MAX_RESULTS,
    private val modelName:String = ModelConstants.MODEL_NAME,
    private val context:Context,
    private val classificationListener: ClassifierListener? = null
) {

    private var imageClassifier:ImageClassifier? = null
    
    init {
        TfLiteGpu.isGpuDelegateAvailable(context).onSuccessTask { gpuAvailable ->
            val optionsBuilder = TfLiteInitializationOptions.builder()
            
            if (gpuAvailable){
                optionsBuilder.setEnableGpuDelegateSupport(true)
            }
            TfLiteVision.initialize(context, optionsBuilder.build())
        }.addOnSuccessListener { 
            setupImageClassifier()
        }.addOnFailureListener { 
            classificationListener?.onError(
                context.getString(R.string.tflite_initialization_with_gpu_delegate_failed)
            )
        }
    }

    private fun setupImageClassifier() {
       val optionsBuilder = ImageClassifierOptions.builder()
           .setScoreThreshold(threshold)
           .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder()

        if(CompatibilityList().isDelegateSupportedOnThisDevice){
            baseOptionsBuilder.useGpu()
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1){
            baseOptionsBuilder.useNnapi()
        }else{
            baseOptionsBuilder.setNumThreads(4)
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelName,
                optionsBuilder.build()
            )
        }catch (e:IllegalStateException){
            classificationListener?.onError(e.message.toString())
            e.printStackTrace()
        }
    }

    fun classifyStaticImage(imageUri: Uri) {
        // TODO: mengklasifikasikan imageUri dari gambar statis.

        if (imageClassifier == null){
            setupImageClassifier()
        }

        val imageBitmap = context.convertImageUriToBitmap(imageUri)

        val imageProcessor = ImageProcessor.Builder()
            .add(
                ResizeOp(
                    ModelConstants.RESIZING_DIMENSION,
                    ModelConstants.RESIZING_DIMENSION, ResizeOp.ResizeMethod.BILINEAR)
            )
            .add(CastOp(DataType.FLOAT32))
            .build()

        val tensorImage = imageProcessor.process(
            TensorImage.fromBitmap(imageBitmap)
        )

        var inferenceTime = SystemClock.uptimeMillis()
        val results = imageClassifier?.classify(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        classificationListener?.onResult(
            results,
            inferenceTime
        )
    }

    interface ClassifierListener{
        fun onResult(
            results:List<Classifications>?,
            inferenceTime:Long
        )

        fun onError(error:String)
    }
}