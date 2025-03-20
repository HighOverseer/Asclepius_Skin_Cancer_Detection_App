package com.dicoding.asclepius.presentation.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.dicoding.asclepius.databinding.FragmentPredictionHistoryBinding
import com.dicoding.asclepius.domain.model.PredictionHistory
import com.dicoding.asclepius.presentation.adapter.PredictionHistoriesAdapter
import com.dicoding.asclepius.presentation.utils.collectLatestOnLifeCycleStarted
import com.dicoding.asclepius.presentation.viewmodel.PredictionHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PredictionHistoryFragment : Fragment() {
    private var binding: FragmentPredictionHistoryBinding? = null
    private val viewModel: PredictionHistoryViewModel by viewModels()
    private val adapter = PredictionHistoriesAdapter(::onItemClick)

    private var settingSearchBarVisibilityJob: Job? = null

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
            viewLifecycleOwner.collectLatestOnLifeCycleStarted(viewModel.predictionHistories){ pagingData ->
                adapter.submitData(
                    lifecycle,
                    pagingData
                )
            }

            rvHistories.addOnScrollListener(object : OnScrollListener(){

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    when(newState){
                        RecyclerView.SCROLL_STATE_DRAGGING -> animateSlide(false)
                        RecyclerView.SCROLL_STATE_IDLE -> animateSlide(true)
                    }
                }
            })
        }
    }

    fun animateSlide(isSlideIn:Boolean){
        binding?.apply {
            val views = listOf(
                lineTop,
                bgSearchBar,
                line,
                searchBar
            )

            val startTranslationValue = if(isSlideIn) -50f else 0f
            val endTranslationValue = if(isSlideIn) 0f else -50f
            val startAlphaValue = if(isSlideIn) 0f else 1f
            val endAlphaValue = if(isSlideIn) 1f else 0f

            val animators = views.flatMap { view ->
                listOf(
                    ObjectAnimator.ofFloat(view, "translationY", startTranslationValue, endTranslationValue).setDuration(500L),
                    ObjectAnimator.ofFloat(view, "alpha", startAlphaValue, endAlphaValue).setDuration(500L)
                )
            }

            AnimatorSet().apply {
                playTogether(animators)
                start()
            }
        }

    }


    private val textWatcher = object:TextWatcher{
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun afterTextChanged(query: Editable?) {
            viewModel.searchHistories(query.toString())
        }
    }

    private fun initView(){
        binding?.apply {
            rvHistories.adapter = adapter
            rvHistories.layoutManager = LinearLayoutManager(requireContext())
            searchBar.setText(viewModel.searchQuery.value)
            searchBar.addTextChangedListener(
                textWatcher
            )
        }
    }

    private fun onItemClick(predictionHistory: PredictionHistory){
        val intent = Intent(requireContext(), ResultActivity::class.java)
        intent.putExtra(ResultActivity.EXTRA_URI, predictionHistory.imageUri)
        intent.putExtra(ResultActivity.EXTRA_OUTPUT, predictionHistory.modelOutput)
        intent.putExtra(ResultActivity.EXTRA_SAVEABLE, false)
        intent.putExtra(ResultActivity.EXTRA_SESSION_NAME, predictionHistory.sessionName)
        intent.putExtra(ResultActivity.EXTRA_DATE, predictionHistory.date)
        startActivity(intent)
    }

    override fun onDestroyView() {
        binding?.searchBar?.removeTextChangedListener(textWatcher)
        super.onDestroyView()
        binding = null
    }
}