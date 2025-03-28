package com.dicoding.asclepius.presentation.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.dicoding.asclepius.databinding.FragmentPredictionHistoryBinding
import com.dicoding.asclepius.domain.model.PredictionHistory
import com.dicoding.asclepius.presentation.adapter.PredictionHistoriesAdapter
import com.dicoding.asclepius.presentation.utils.collectLatestOnLifeCycleStarted
import com.dicoding.asclepius.presentation.viewmodel.PredictionHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PredictionHistoryFragment : Fragment() {
    private var binding: FragmentPredictionHistoryBinding? = null
    private val viewModel: PredictionHistoryViewModel by viewModels()
    private val adapter = PredictionHistoriesAdapter(::onItemClick)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPredictionHistoryBinding.inflate(
            inflater,
            container,
            false
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()

        binding?.apply {
            viewLifecycleOwner.collectLatestOnLifeCycleStarted(viewModel.predictionHistories) { pagingData ->
                adapter.submitData(
                    lifecycle,
                    pagingData
                )
            }
        }
    }

    private val rvScrollListener = object : OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            when (newState) {
                RecyclerView.SCROLL_STATE_DRAGGING -> animateSlide(false)
                RecyclerView.SCROLL_STATE_IDLE -> animateSlide(true)
            }
        }
    }

    private val adapterLoadStateListener = { state: CombinedLoadStates ->
        val isEmptyTextViewVisible = state.refresh is LoadState.NotLoading && adapter.itemCount == 0
        binding?.tvEmpty?.isVisible = isEmptyTextViewVisible

    }

    fun animateSlide(isSlideIn: Boolean) {
        binding?.apply {
            val views = listOf(
                lineTop,
                bgSearchBar,
                line,
                searchBar
            )

            val startTranslationValue = if (isSlideIn) -50f else 0f
            val endTranslationValue = if (isSlideIn) 0f else -50f
            val startAlphaValue = if (isSlideIn) 0f else 1f
            val endAlphaValue = if (isSlideIn) 1f else 0f

            val animators = views.flatMap { view ->
                listOf(
                    ObjectAnimator.ofFloat(
                        view,
                        TRANSLATION_Y_ANIMATION_PROPERTY,
                        startTranslationValue,
                        endTranslationValue
                    ).setDuration(SEARCH_BAR_ANIMATION_DURATION),
                    ObjectAnimator.ofFloat(
                        view,
                        ALPHA_ANIMATION_PROPERTY,
                        startAlphaValue,
                        endAlphaValue
                    )
                        .setDuration(SEARCH_BAR_ANIMATION_DURATION)
                )
            }

            AnimatorSet().apply {
                playTogether(animators)
                start()
            }
        }

    }


    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun afterTextChanged(query: Editable?) {
            viewModel.searchHistories(query.toString())
        }
    }

    private fun initView() {
        binding?.apply {
            rvHistories.adapter = adapter
            rvHistories.layoutManager = LinearLayoutManager(requireContext())
            searchBar.setText(viewModel.searchQuery.value)
            searchBar.addTextChangedListener(
                textWatcher
            )
            adapter.addLoadStateListener(adapterLoadStateListener)
            rvHistories.addOnScrollListener(rvScrollListener)
        }
    }

    private fun onItemClick(predictionHistory: PredictionHistory) {
        Intent(requireContext(), ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_URI, predictionHistory.imageUri)
            putExtra(ResultActivity.EXTRA_OUTPUT, predictionHistory.modelOutput)
            putExtra(ResultActivity.EXTRA_SAVEABLE, false)
            putExtra(ResultActivity.EXTRA_SESSION_NAME, predictionHistory.sessionName)
            putExtra(ResultActivity.EXTRA_DATE, predictionHistory.date)
            putExtra(ResultActivity.EXTRA_NOTE, predictionHistory.note)
            startActivity(this)
        }
    }

    override fun onDestroyView() {
        binding?.searchBar?.removeTextChangedListener(textWatcher)
        binding?.rvHistories?.removeOnScrollListener(rvScrollListener)
        adapter.removeLoadStateListener(adapterLoadStateListener)
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val TRANSLATION_Y_ANIMATION_PROPERTY = "translationY"
        private const val ALPHA_ANIMATION_PROPERTY = "alpha"
        private const val SEARCH_BAR_ANIMATION_DURATION = 500L
    }
}