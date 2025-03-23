package com.dicoding.asclepius.presentation.view

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityResult2Binding
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.presentation.uievent.ResultUIEvent
import com.dicoding.asclepius.presentation.utils.collectChannelFlowWhenStarted
import com.dicoding.asclepius.presentation.utils.collectLatestOnLifeCycleStarted
import com.dicoding.asclepius.presentation.utils.convertImageUriToReducedBitmap
import com.dicoding.asclepius.presentation.utils.deleteFromFileProvider
import com.dicoding.asclepius.presentation.utils.formatToPercentage
import com.dicoding.asclepius.presentation.utils.getColorFromAttr
import com.dicoding.asclepius.presentation.utils.loadImage
import com.dicoding.asclepius.presentation.utils.showToast
import com.dicoding.asclepius.presentation.viewmodel.ResultViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResultActivity : AppCompatActivity(), SessionDialogFragment.OnDismissListener {
    private lateinit var binding: ActivityResult2Binding

    private var loadImageJob: Job? = null

    private val viewModel: ResultViewModel by viewModels()

    private var didUserSavedTheSession: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityResult2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)


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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
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

            toolbarLayout.setExpandedTitleColor(getColorFromAttr(android.R.attr.colorPrimary))
            toolbarLayout.setCollapsedTitleTextColor(resources.getColor(R.color.white_100, null))
            loadImageJob?.cancel()
            loadImageJob = lifecycleScope.launch {
                val compressedBitmap = this@ResultActivity
                    .convertImageUriToReducedBitmap(imageUri)
                resultImage.loadImage(compressedBitmap)
                content.resultText.text = output.confidenceScore.formatToPercentage()
                content.tvLabel.text = output.label
            }

//            ibBack.setOnClickListener {
//                finish()
//            }


            if (isSaveAble) {
                content.tvInfoDate.isVisible = false
                content.tvDate.isVisible = false
                toolbarLayout.title = getString(R.string.hasil_analisis)
                fabSave.setOnClickListener {
                    viewModel.sendEvent(ResultUIEvent.ShowSessionDialog)
                }
            } else {

                val sessionName = viewModel.latestSessionName
                val sessionDate = viewModel.latestSessionDate

                if (sessionName == null || sessionDate == null) return

                //TODO()
                toolbarLayout.title = sessionName
                content.tvDate.text = sessionDate
//                tvSessionName.isVisible = true
//                tvSessionDate.isVisible = true
//
//                tvSessionName.text = sessionName
//                tvSessionDate.text = sessionDate
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