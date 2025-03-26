package com.dicoding.asclepius.ml

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import com.dicoding.asclepius.R
import com.dicoding.asclepius.domain.common.StringRes
import com.dicoding.asclepius.domain.model.ModelOutput
import com.dicoding.asclepius.domain.presentation.ClassifierListener
import com.dicoding.asclepius.domain.presentation.ImageClassifierHelper
import com.dicoding.asclepius.presentation.utils.convertImageUriToBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.gms.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.gms.vision.classifier.ImageClassifier.ImageClassifierOptions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageClassifierHelperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageClassifierHelper {

    private var classificationListener: ClassifierListener? = null

    private var imageClassifier: ImageClassifier? = null

    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifierOptions.builder()
            .setScoreThreshold(ModelConstants.THRESHOLD)
            .setMaxResults(ModelConstants.MAX_RESULTS)

        val baseOptionsBuilder = BaseOptions.builder()

        if (CompatibilityList().isDelegateSupportedOnThisDevice) {
            baseOptionsBuilder.useGpu()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            baseOptionsBuilder.useNnapi()
        } else {
            baseOptionsBuilder.setNumThreads(4)
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                ModelConstants.MODEL_NAME,
                optionsBuilder.build()
            )
        } catch (e: IllegalStateException) {
            classificationListener?.onError(
                StringRes.Dynamic(e.message.toString())
            )
            e.printStackTrace()
        }
    }

    override fun classifyStaticImage(imageUriPath: String) {
        val imageUri = Uri.parse(imageUriPath) ?: return

        if (imageClassifier == null) {
            setupImageClassifier()
        }

        val imageBitmap = context.convertImageUriToBitmap(imageUri)

        val imageProcessor = ImageProcessor.Builder()
            .add(
                ResizeOp(
                    ModelConstants.RESIZING_DIMENSION,
                    ModelConstants.RESIZING_DIMENSION, ResizeOp.ResizeMethod.BILINEAR
                )
            )
            .add(CastOp(DataType.FLOAT32))
            .build()

        val tensorImage = imageProcessor.process(
            TensorImage.fromBitmap(imageBitmap)
        )

        var inferenceTime = SystemClock.uptimeMillis()
        val results = imageClassifier?.classify(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        results?.let {
            if (it.isNotEmpty() && it[0].categories.isNotEmpty()) {
                val sortedCategories = it[0].categories.sortedByDescending { category ->
                    category?.score
                }
                val outputs = sortedCategories.map { category ->
                    ModelOutput(
                        label = category.label,
                        confidenceScore = category.score
                    )
                }
                classificationListener?.onResult(
                    imageUri,
                    outputs,
                    inferenceTime
                )
            } else {
                classificationListener?.onError(
                    StringRes.Static(R.string.result_empty_please_try_again)
                )
            }
        } ?: classificationListener?.onError(
            StringRes.Static(R.string.result_empty_please_try_again)
        )
    }

    override fun setClassificationListener(listener: ClassifierListener) {
        classificationListener = listener
    }
}