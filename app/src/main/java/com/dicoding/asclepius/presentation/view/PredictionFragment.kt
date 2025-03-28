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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.FragmentPredictionBinding
import com.dicoding.asclepius.domain.model.ModelOutput
import com.dicoding.asclepius.presentation.uievent.PredictionUIEvent
import com.dicoding.asclepius.presentation.utils.cancelRequest
import com.dicoding.asclepius.presentation.utils.collectChannelFlowWhenStarted
import com.dicoding.asclepius.presentation.utils.collectLatestOnLifeCycleStarted
import com.dicoding.asclepius.presentation.utils.getColorFromAttr
import com.dicoding.asclepius.presentation.utils.getValue
import com.dicoding.asclepius.presentation.utils.image.CroppedImageResult
import com.dicoding.asclepius.presentation.utils.image.ImageCompressor
import com.dicoding.asclepius.presentation.utils.image.ImageConverter
import com.dicoding.asclepius.presentation.utils.image.PickImageMediaResult
import com.dicoding.asclepius.presentation.utils.loadImage
import com.dicoding.asclepius.presentation.utils.showToast
import com.dicoding.asclepius.presentation.utils.ui.UCropActivityResultHandler
import com.dicoding.asclepius.presentation.viewmodel.PredictionViewModel
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PredictionFragment : Fragment(), UCropActivityResultHandler.OnHandleResult {
    private val imageCompressor by lazy { ImageCompressor() }

    @Inject
    lateinit var imageConverter: ImageConverter
    private var loadImageJob: Job? = null
    private val uCropActivityResultHandler = UCropActivityResultHandler(this)

    private var binding: FragmentPredictionBinding? = null
    private val viewModel: PredictionViewModel by viewModels()

    private val colorPrimary by lazy { requireContext().getColorFromAttr(android.R.attr.colorPrimary) }
    private val white100 by lazy { ContextCompat.getColor(requireContext(), R.color.white_100) }
    private val black50 by lazy { ContextCompat.getColor(requireContext(), R.color.black_50) }
    private val darkerGray by lazy { ContextCompat.getColor(requireContext(), R.color.grey_200) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    if (requireActivity().isFinishing) {
                        binding?.previewImageView?.cancelRequest()
                        viewModel.clearingAllUnusedImageUris()
                    }
                }

                else -> return
            }
        }
    }

    override fun onSuccess(resultImageUri: Uri) {
        viewModel.handleOnResultCroppedImageSession(
            CroppedImageResult.Success(resultImageUri.toString())
        )
        showToast(getString(R.string.taking_image_successfully))
    }

    override fun onError(t: Throwable) {
        viewModel.handleOnResultCroppedImageSession(
            CroppedImageResult.Failed
        )
        showToast(t.message.toString())
    }

    override fun onCanceled() {
        viewModel.handleOnResultCroppedImageSession(
            CroppedImageResult.Canceled
        )
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
                viewModel.analyzeImage()
            }

            viewLifecycleOwner.collectLatestOnLifeCycleStarted(viewModel.uiState) { uiState ->
                val imageUri = uiState.currentImageUriPath?.let { Uri.parse(it) }
                if (uiState.isReadyToShowLatestImage) showImage(imageUri)

                val latestIsLoading = progressIndicator.isShown
                if (uiState.isInClassifyingProcess == latestIsLoading || uiState.isStillInitializingTFLiteVision == latestIsLoading) return@collectLatestOnLifeCycleStarted

                if (uiState.isStillInitializingTFLiteVision || uiState.isInClassifyingProcess) {
                    progressIndicator.show()
                } else progressIndicator.hide()
            }

            viewLifecycleOwner.collectChannelFlowWhenStarted(viewModel.uiEvent) {
                when (it) {
                    is PredictionUIEvent.OnClassificationFailed -> {
                        showToast(it.message.getValue(requireContext()))
                    }

                    is PredictionUIEvent.OnClassificationSuccess -> {
                        moveToResult(it.imageUri, it.modelOutput)
                    }

                    is PredictionUIEvent.OnPickingPreCroppedImageSuccessfully -> {
                        moveToCropActivity(it.imageUriPath)
                    }

                    is PredictionUIEvent.FailedStartingAnalyzingProcess -> {
                        showToast(getString(R.string.still_initializing_prediction_process))
                    }
                }
            }
        }
    }

    private fun initTvDescription() {
        binding?.apply {
            val descriptionParts = requireContext()
                .applicationContext
                .resources
                .getStringArray(R.array.analyze_descriptions)

            val descriptionMaps = hashMapOf(
                1 to colorPrimary, 3 to colorPrimary, 5 to black50
            )

            val description = SpannableStringBuilder().apply {
                for (i in descriptionParts.indices) {
                    val start = length
                    append(descriptionParts[i])

                    val colorForThisPart = descriptionMaps[i]

                    setSpan(
                        ForegroundColorSpan(colorForThisPart ?: darkerGray),
                        start,
                        length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    if (colorForThisPart != null) {
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

    @Suppress("DEPRECATION")
    @Deprecated(
        "Deprecated in Java", ReplaceWith(
            "super.onActivityResult(requestCode, resultCode, data)",
            "androidx.appcompat.app.AppCompatActivity"
        )
    )
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        uCropActivityResultHandler.onActivityResult(
            requestCode = requestCode,
            resultCode = resultCode,
            data = data
        )
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
            viewModel.handleOnPickImageMediaResult(PickImageMediaResult.Failed)
            return@registerForActivityResult
        }

        viewModel.handleOnPickImageMediaResult(
            PickImageMediaResult.CameraSuccess
        )
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { imageUri ->
        val isImageSelected = imageUri != null
        if (isImageSelected) {
            viewModel.handleOnPickImageMediaResult(
                PickImageMediaResult.GallerySuccess(imageUri.toString())
            )
        } else {
            viewModel.handleOnPickImageMediaResult(PickImageMediaResult.Failed)
        }
    }

    private val resultActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.resetSession()

        val isSessionSaved = result.resultCode == FLAG_IS_SESSION_SAVED
        (requireActivity() as? OnBackFromResultActivityCallback)?.onBackFromResult(isSessionSaved)
    }

    private fun moveToCropActivity(imageUriPath: String) {
        val croppedImageFile = viewModel.provideForCroppedImageFile()
        val currImageUri = Uri.parse(imageUriPath) ?: return

        UCrop.of(currImageUri, Uri.fromFile(croppedImageFile))
            .apply {
                val options = UCrop.Options().apply {
                    setToolbarColor(colorPrimary)
                    setCropFrameColor(colorPrimary)
                    setToolbarWidgetColor(white100)
                    setActiveControlsWidgetColor(colorPrimary)
                    setCropGridColor(colorPrimary)
                }
                withOptions(options)
                viewModel.latestCroppedImageFilePath = croppedImageFile.absolutePath
                this.start(requireContext(), this@PredictionFragment)
            }

    }


    private fun showImage(imageUri: Uri?) {
        loadImageJob?.cancel()

        if (imageUri != null) {
            loadImageJob = viewLifecycleOwner.lifecycleScope.launch {
                val compressedBitmap = imageConverter.convertImageUriToReducedBitmap(
                    imageUri, imageCompressor
                )

                binding?.previewImageView?.loadImage(compressedBitmap)
            }

            return
        }

        val uri: Uri? = null
        binding?.previewImageView?.loadImage(uri)
    }

    private fun startGallery() {
        viewModel.prepareForPickingUpAnImageFromMedia()
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
            viewModel.prepareForPickingUpAnImageFromMedia()

            val uri = viewModel.provideCapturedImageUriForNewSession()
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
        resultActivityLauncher.launch(intent)
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

    interface OnBackFromResultActivityCallback {
        fun onBackFromResult(isSessionSaved: Boolean)
    }

    companion object {
        const val FLAG_IS_SESSION_SAVED = 100
    }
}