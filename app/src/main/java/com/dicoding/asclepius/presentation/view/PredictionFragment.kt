package com.dicoding.asclepius.presentation.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.FragmentPredictionBinding
import com.dicoding.asclepius.presentation.utils.ImageCaptureHandler
import com.dicoding.asclepius.ml.ImageClassifierHelper
import com.dicoding.asclepius.presentation.utils.convertImageUriToReducedBitmap
import com.dicoding.asclepius.presentation.utils.loadImage
import com.dicoding.asclepius.presentation.utils.showToast
import com.dicoding.asclepius.domain.model.ModelOutput
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.gms.vision.classifier.Classifications
import java.io.File

@AndroidEntryPoint
class PredictionFragment : Fragment(), ImageClassifierHelper.ClassifierListener {
    private var binding:FragmentPredictionBinding? = null

    private var currentImageUri: Uri? = null
    private val imageCaptureHandler = ImageCaptureHandler()

    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private var loadImageJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageClassifierHelper = ImageClassifierHelper(
            context = requireContext().applicationContext,
            classificationListener = this
        )
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    @Deprecated("Deprecated in Java", ReplaceWith(
        "super.onActivityResult(requestCode, resultCode, data)",
        "androidx.appcompat.app.AppCompatActivity"
    )
    )
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(data == null) return

        if(resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP){
            val resultImageUri = UCrop.getOutput(data)
            currentImageUri = resultImageUri
            showImage()

        }else if(resultCode == UCrop.RESULT_ERROR){
            // so user can press the analyze button
            currentImageUri = null

            val error = UCrop.getError(data)
            error?.let {
                showToast(it.message.toString())
            }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ isGranted ->
        val message = if(isGranted){
            getString(R.string.permission_granted_will_proceed)
        }else{
            getString(R.string.need_permission_to_use_this_feature)
        }
        showToast(message)

        if(isGranted){
            startCamera()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ){ isSuccess ->
        if(!isSuccess) return@registerForActivityResult

        currentImageUri = imageCaptureHandler.latestImageCaptured

        moveToCropActivity()
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ){ imageUri ->
        currentImageUri = imageUri

        moveToCropActivity()
    }


    private fun moveToCropActivity(){
        currentImageUri?.let {
            UCrop.of(it, Uri.fromFile(
                File(requireContext().cacheDir, "cropped_image.jpg")))
                .apply { this.start(requireContext(), this@PredictionFragment) }
        }
    }

    private fun analyzeImage() {
        // TODO: Menganalisa gambar yang berhasil ditampilkan.
        val imageUri = currentImageUri
        if (imageUri == null){
            showToast(getString(R.string.please_select_an_image_first))
            return
        }

        imageClassifierHelper.classifyStaticImage(imageUri)
    }

    override fun onResult(results: List<Classifications>?, inferenceTime: Long) {
        requireActivity().runOnUiThread {
            results?.let {
                if(it.isNotEmpty() && it[0].categories.isNotEmpty()){
                    val sortedCategories = it[0].categories.sortedByDescending {
                            category -> category?.score
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
        galleryLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    private fun startCamera(){
        val isCameraPermissionGranted =
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if(!isCameraPermissionGranted){
            askCameraPermission()
        }else{
            val uri = imageCaptureHandler.getImageUri(requireContext().applicationContext)
            cameraLauncher.launch(uri)
        }
    }

    private fun moveToResult(imageUri: Uri, output: ModelOutput) {
        val intent = Intent(requireActivity(), ResultActivity::class.java)
        intent.apply {
            putExtra(ResultActivity.EXTRA_URI, imageUri.toString())
            putExtra(ResultActivity.EXTRA_OUTPUT, output)
            putExtra(ResultActivity.EXTRA_SAVEABLE, true)
        }

        startActivity(intent)
    }

    private fun askCameraPermission(){
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}