package com.dicoding.asclepius.presentation.view

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
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
import java.lang.ref.WeakReference

@AndroidEntryPoint
class ResultActivity : AppCompatActivity(), SessionDialogFragment.OnDialogEventListener {
    private lateinit var binding: ActivityResultBinding

    private var loadImageJob: Job? = null

    private val viewModel: ResultViewModel by viewModels()

    private var didUserSavedTheSession: Boolean = false

    private var progressIndicatorAnimation: WeakReference<ObjectAnimator>? = null

    private var transientIsActivityOnConfigurationChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

                    else -> return@collectChannelFlowWhenStarted
                }
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        transientIsActivityOnConfigurationChanged = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        transientIsActivityOnConfigurationChanged = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        val imageUri = viewModel.latestImageUri?.let { Uri.parse(it) }
        val output = viewModel.latestModelOutput
        val isSaveAble = viewModel.isSaveAble

        if (imageUri == null || output == null) return

        binding.apply {
            content.apply {
                toolbarLayout.setExpandedTitleColor(
                    resources.getColor(
                        R.color.black_50,
                        null
                    )
                )
                toolbarLayout.setCollapsedTitleTextColor(
                    resources.getColor(
                        R.color.white_100,
                        null
                    )
                )
                loadImageJob?.cancel()
                loadImageJob = lifecycleScope.launch {
                    val compressedBitmap = this@ResultActivity
                        .convertImageUriToReducedBitmap(imageUri)
                    resultImage.loadImage(compressedBitmap)
                    resultText.text = output.confidenceScore.formatToPercentage()
                    tvLabel.text = output.label

                    val progress = (output.confidenceScore * 100).toInt()

                    ObjectAnimator.ofInt(progressCircular, PROGRESS_INDICATOR_PROPERTY, 0, progress)
                        .apply {
                            progressIndicatorAnimation = WeakReference(this)
                            duration = PROGRESS_INDICATOR_ANIMATION_DURATION
                            start()
                        }
                }

                if (isSaveAble) {
                    tvInfoDate.isVisible = false
                    tvDate.isVisible = false
                    noteEtBg.isVisible = true
                    etNote.isVisible = true
                    noteTvBg.isVisible = false
                    tvNote.isVisible = false

                    toolbarLayout.title = getString(R.string.hasil_analisis)
                    fabSave.setOnClickListener {
                        viewModel.sendEvent(ResultUIEvent.ShowSessionDialog)
                    }
                } else {
                    tvInfoDate.isVisible = true
                    tvDate.isVisible = true
                    noteEtBg.isVisible = false
                    etNote.isVisible = false
                    noteTvBg.isVisible = true
                    tvNote.isVisible = true

                    val sessionName = viewModel.latestSessionName
                    val sessionDate = viewModel.latestSessionDate
                    val sessionNote = viewModel.latestSessionNote?.ifBlank { "-" }

                    if (sessionName == null || sessionDate == null || sessionNote == null) return

                    toolbarLayout.title = sessionName
                    tvDate.text = sessionDate
                    tvNote.text = sessionNote
                }
            }
        }
    }

    override fun onDestroy() {
        progressIndicatorAnimation?.get()?.cancel()
        val imageUri = viewModel.latestImageUri
        val isSaveAble = viewModel.isSaveAble
        if (!didUserSavedTheSession && imageUri != null && isSaveAble && !transientIsActivityOnConfigurationChanged) {
            deleteFromFileProvider(this, uri = Uri.parse(imageUri))
        }
        super.onDestroy()
    }

    private fun showDialog() {
        val dialogFragment = SessionDialogFragment()
        dialogFragment.show(supportFragmentManager, null)
    }

    override fun onDismiss(sessionName: String) {
        val note = binding.content.etNote.text.toString().trim()
        viewModel.insertPredictionHistory(sessionName, note)
    }

    override fun onCancel() {
        viewModel.sendEvent(ResultUIEvent.SessionDialogCanceled)
    }

    companion object {
        const val EXTRA_OUTPUT = "extra_output"
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_SAVEABLE = "extra_saveable"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_SESSION_NAME = "extra_session_name"
        const val EXTRA_NOTE = "extra_note"

        private const val PROGRESS_INDICATOR_PROPERTY = "progress"
        private const val PROGRESS_INDICATOR_ANIMATION_DURATION = 1000L
    }
}