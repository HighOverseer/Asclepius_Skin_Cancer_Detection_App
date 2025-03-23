package com.dicoding.asclepius.presentation.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.FragmentPredictionBinding
import com.dicoding.asclepius.domain.model.ModelOutput
import com.dicoding.asclepius.ml.ImageClassifierHelper
import com.dicoding.asclepius.presentation.utils.ImageCaptureHandler
import com.dicoding.asclepius.presentation.utils.cancelRequest
import com.dicoding.asclepius.presentation.utils.convertImageUriToReducedBitmap
import com.dicoding.asclepius.presentation.utils.deleteFromFileProvider
import com.dicoding.asclepius.presentation.utils.getColorFromAttr
import com.dicoding.asclepius.presentation.utils.getFile
import com.dicoding.asclepius.presentation.utils.getFileName
import com.dicoding.asclepius.presentation.utils.loadImage
import com.dicoding.asclepius.presentation.utils.showToast
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.gms.vision.classifier.Classifications
import java.io.File

@AndroidEntryPoint
class PredictionFragment : Fragment(), ImageClassifierHelper.ClassifierListener {
    private var binding: FragmentPredictionBinding? = null

    private var currentImageUri: Uri? = null

    //when user click cancel button on ucrop activity, see OnActivityResult
    private var latestCroppedImageFilePath: String? = null

    private val imageCaptureHandler = ImageCaptureHandler()

    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private var loadImageJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageClassifierHelper = ImageClassifierHelper(
            context = requireContext().applicationContext,
            classificationListener = this
        )

        requireActivity().lifecycle.addObserver(activityObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPredictionBinding.inflate(
            inflater,
            container,
            false
        )
        return binding?.root
    }

    private val activityObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_DESTROY -> {
                    binding?.previewImageView?.cancelRequest()
                    imageCaptureHandler.clearLatestCapturedImageUri(
                        requireContext()
                    )
                    currentImageUri?.let {
                        deleteFromFileProvider(requireContext().applicationContext, it)
                    }
                    currentImageUri = null
                }

                else -> return
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTvDescription()
        binding?.apply {
            galleryButton.setOnClickListener {
                startGallery()
            }
            cameraButton.setOnClickListener {
                startCamera()
            }
            analyzeButton.setOnClickListener {
                analyzeImage()
            }
        }
    }

    private fun initTvDescription() {
        binding?.apply {
            val descriptionParts = requireContext()
                .applicationContext
                .resources
                .getStringArray(R.array.analyze_descriptions)


            val colorPrimary = requireContext()
                .getColorFromAttr(android.R.attr.colorPrimary)
            val black50 = ContextCompat.getColor(requireContext(), R.color.black_50)
            val darkerGray = ContextCompat.getColor(requireContext(), R.color.grey_200)

            val descriptionMaps = hashMapOf(
                1 to colorPrimary, 3 to colorPrimary, 5 to black50
            )

            val description = SpannableStringBuilder().apply {
                for(i in descriptionParts.indices){
                    val start = length
                    append(descriptionParts[i])

                    val colorForThisPart = descriptionMaps[i]

                    setSpan(
                        ForegroundColorSpan(colorForThisPart ?: darkerGray),
                        start,
                        length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    if(colorForThisPart != null) {
                        setSpan(
                            StyleSpan(android.graphics.Typeface.BOLD),
                            start,
                            length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    append(" ")
                }
            }
            tvDescription.text = description

        }
    }

    @Deprecated(
        "Deprecated in Java", ReplaceWith(
            "super.onActivityResult(requestCode, resultCode, data)",
            "androidx.appcompat.app.AppCompatActivity"
        )
    )
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        imageCaptureHandler.clearLatestCapturedImageUri(
            requireContext().applicationContext
        )

        when {
            resultCode == RESULT_CANCELED -> {
                latestCroppedImageFilePath?.let {
                    val canceledCroppedImageFile = File(it)
                    if (canceledCroppedImageFile.exists()) canceledCroppedImageFile.delete()
                }
                latestCroppedImageFilePath = null
            }

            resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP -> {
                if (data == null) return

                val resultImageUri = UCrop.getOutput(data) ?: return
                currentImageUri = resultImageUri
                showToast(getString(R.string.taking_image_successfully))
                showImage()
            }

            resultCode == UCrop.RESULT_ERROR -> {
                if (data == null) return

                val error = UCrop.getError(data)
                error?.let {
                    currentImageUri = null
                    showToast(it.message.toString())
                }
            }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val message = if (isGranted) {
            getString(R.string.permission_granted_will_proceed)
        } else {
            getString(R.string.need_permission_to_use_this_feature)
        }
        showToast(message)

        if (isGranted) {
            startCamera()
        }
    }


    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (!isSuccess) {
            imageCaptureHandler.clearLatestCapturedImageUri(
                requireContext().applicationContext
            )
            return@registerForActivityResult
        }

        currentImageUri = imageCaptureHandler.latestImageCapturedUri

        moveToCropActivity()
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { imageUri ->
        val isImageSelected = imageUri != null
        if (isImageSelected) {
            currentImageUri = imageUri
            moveToCropActivity()
        } else {
            clearSession()
        }

    }


    private fun moveToCropActivity() {
        val croppedImageFile = getFile(requireContext(), getFileName())
        val currImageUri = currentImageUri ?: return

        UCrop.of(currImageUri, Uri.fromFile(croppedImageFile))
            .apply {
                latestCroppedImageFilePath = croppedImageFile.absolutePath
                this.start(requireContext(), this@PredictionFragment)
            }

    }

    private fun analyzeImage() {
        // TODO: Menganalisa gambar yang berhasil ditampilkan.
        val imageUri = currentImageUri
        if (imageUri == null) {
            showToast(getString(R.string.please_select_an_image_first))
            return
        }

        imageClassifierHelper.classifyStaticImage(imageUri)
    }

    override fun onResult(results: List<Classifications>?, inferenceTime: Long) {
        requireActivity().runOnUiThread {
            results?.let {
                if (it.isNotEmpty() && it[0].categories.isNotEmpty()) {
                    val sortedCategories = it[0].categories.sortedByDescending { category ->
                        category?.score
                    }
                    val output = sortedCategories.first().run {
                        ModelOutput(
                            label = label,
                            confidenceScore = score
                        )
                    }
                    currentImageUri?.let { imageUri ->
                        moveToResult(imageUri, output)
                    }
                }
            }
        }
    }

    override fun onError(error: String) {
        showToast(error)
    }

    private fun showImage() {
        // TODO: Menampilkan gambar sesuai Gallery yang dipilih.
        currentImageUri?.let {
            loadImageJob?.cancel()
            loadImageJob = viewLifecycleOwner.lifecycleScope.launch {
                val compressedBitmap = requireContext()
                    .applicationContext
                    .convertImageUriToReducedBitmap(it)
                binding?.previewImageView?.loadImage(compressedBitmap)
            }
        }
    }

    private fun startGallery() {
        // TODO: Mendapatkan gambar dari Gallery.
        currentImageUri?.let {
            deleteFromFileProvider(requireContext(), it)
            clearSession()
        }

        galleryLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    private fun startCamera() {
        val isCameraPermissionGranted =
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (!isCameraPermissionGranted) {
            askCameraPermission()
        } else {
            currentImageUri?.let {
                deleteFromFileProvider(requireContext(), it)
                clearSession()
            }

            val uri =
                imageCaptureHandler.provideCapturedImageUri(requireContext().applicationContext)
            cameraLauncher.launch(uri)
        }
    }

    private fun moveToResult(imageUri: Uri, output: ModelOutput) {
        val activity = requireActivity()

        if (activity !is MainActivity) return

        val intent = Intent(requireActivity(), ResultActivity::class.java)
        intent.apply {
            putExtra(ResultActivity.EXTRA_URI, imageUri.toString())
            putExtra(ResultActivity.EXTRA_OUTPUT, output)
            putExtra(ResultActivity.EXTRA_SAVEABLE, true)
        }
        activity.resultActivityLauncher.launch(intent)
    }

    private fun askCameraPermission() {
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().lifecycle.removeObserver(activityObserver)
    }

    fun clearSession() {
        currentImageUri = null
        binding?.previewImageView?.setImageDrawable(null)
    }


    companion object {
        const val FLAG_IS_SESSION_SAVED = 100
    }
}