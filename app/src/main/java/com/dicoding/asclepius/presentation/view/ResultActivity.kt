package com.dicoding.asclepius.presentation.view

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.presentation.uievent.ResultUIEvent
import com.dicoding.asclepius.presentation.utils.collectChannelFlowWhenStarted
import com.dicoding.asclepius.presentation.utils.collectLatestOnLifeCycleStarted
import com.dicoding.asclepius.presentation.utils.convertImageUriToReducedBitmap
import com.dicoding.asclepius.presentation.utils.deleteFromFileProvider
import com.dicoding.asclepius.presentation.utils.formatToPercentage
import com.dicoding.asclepius.presentation.utils.loadImage
import com.dicoding.asclepius.presentation.utils.showToast
import com.dicoding.asclepius.presentation.viewmodel.ResultViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResultActivity : AppCompatActivity(), SessionDialogFragment.OnDismissListener {
    private lateinit var binding: ActivityResultBinding

    private var loadImageJob: Job? = null

    private val viewModel: ResultViewModel by viewModels()

    private var didUserSavedTheSession: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // TODO: Menampilkan hasil gambar, prediksi, dan confidence score.
        initView()
        binding.apply {


            collectLatestOnLifeCycleStarted(viewModel.uiState) {
                val isSaveAble = viewModel.isSaveAble
                val isFabVisible = !it.isLoading && !it.isShowingDialog && isSaveAble
                fabSave.isVisible = isFabVisible
            }

            collectChannelFlowWhenStarted(viewModel.uiEvent) {
                when (it) {
                    ResultUIEvent.ShowSessionDialog -> {
                        showDialog()
                    }

                    ResultUIEvent.SuccessSavingHistory -> {
                        didUserSavedTheSession = true
                        setResult(PredictionFragment.FLAG_IS_SESSION_SAVED)
                        showToast(getString(R.string.prediction_result_is_saved))
                        finish()
                    }

                    ResultUIEvent.FailedGettingProperExtras -> {
                        showToast(getString(R.string.sorry_something_went_wrong_pleasy_try_again))
                        finish()
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        val imageUri = viewModel.latestImageUri?.let { Uri.parse(it) }
        val output = viewModel.latestModelOutput
        val isSaveAble = viewModel.isSaveAble

        if (imageUri == null || output == null) return

        binding.apply {
            loadImageJob?.cancel()
            loadImageJob = lifecycleScope.launch {
                val compressedBitmap = this@ResultActivity
                    .convertImageUriToReducedBitmap(imageUri)
                resultImage.loadImage(compressedBitmap)
                resultText.text = "${output.label} " + output.confidenceScore.formatToPercentage()
            }

            ibBack.setOnClickListener {
                finish()
            }


            if (isSaveAble) {
                fabSave.setOnClickListener {
                    viewModel.sendEvent(ResultUIEvent.ShowSessionDialog)
                }
            } else {
                val sessionName = viewModel.latestSessionName
                val sessionDate = viewModel.latestSessionDate

                if (sessionName == null || sessionDate == null) return

                tvSessionName.isVisible = true
                tvSessionDate.isVisible = true

                tvSessionName.text = sessionName
                tvSessionDate.text = sessionDate
            }
        }
    }

    override fun onDestroy() {
        val imageUri = viewModel.latestImageUri
        val isSaveAble = viewModel.isSaveAble
        if (!didUserSavedTheSession && imageUri != null && isSaveAble) {
            deleteFromFileProvider(this, uri = Uri.parse(imageUri))
        }
        super.onDestroy()
    }

    private fun showDialog() {
        val dialogFragment = SessionDialogFragment()
        dialogFragment.show(supportFragmentManager, null)
    }

    override fun onDismiss(sessionName: String) {
        viewModel.insertPredictionHistory(sessionName)
    }

    companion object {
        const val EXTRA_OUTPUT = "extra_output"
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_SAVEABLE = "extra_saveable"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_SESSION_NAME = "extra_session_name"
    }
}