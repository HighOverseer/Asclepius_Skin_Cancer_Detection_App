package com.dicoding.asclepius.presentation.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.dicoding.asclepius.databinding.FragmentInformationBinding
import com.dicoding.asclepius.domain.model.CancerNewsPreview
import com.dicoding.asclepius.presentation.adapter.CancerNewsAdapter
import com.dicoding.asclepius.presentation.uievent.InformationUIEvent
import com.dicoding.asclepius.presentation.utils.CancerNewsItemDecoration
import com.dicoding.asclepius.presentation.utils.collectChannelFlowWhenStarted
import com.dicoding.asclepius.presentation.utils.collectLatestOnLifeCycleStarted
import com.dicoding.asclepius.presentation.utils.getValue
import com.dicoding.asclepius.presentation.utils.showToast
import com.dicoding.asclepius.presentation.viewmodel.InformationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InformationFragment : Fragment() {
    private var binding: FragmentInformationBinding? = null
    private val viewModel: InformationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInformationBinding.inflate(
            inflater,
            container,
            false
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        initViews()
        viewLifecycleOwner.collectChannelFlowWhenStarted(viewModel.uiEvent) { event ->
            when (event) {
                is InformationUIEvent.SuccessLoadingInitialData -> {
                    event.message?.getValue(requireContext())?.let {
                        showToast(it)
                    }
                }

                is InformationUIEvent.FailedLoadingInitialData -> {
                    showToast(event.message.getValue(requireContext()))
                }
            }
        }

        viewLifecycleOwner.collectLatestOnLifeCycleStarted(viewModel.isLoading){ isLoading ->
            binding?.apply {
                if(isLoading) linearProgressIndicator.show() else  linearProgressIndicator.hide()
            }
        }


        viewLifecycleOwner.collectLatestOnLifeCycleStarted(viewModel.cancerNews) { cancerNews ->
            binding?.apply {
                val adapter = CancerNewsAdapter(cancerNews, ::onCancerNewsItemClicked)
                rvScreen.swapAdapter(adapter, true)
                rvScreen.layoutManager =
                    GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
                        .also { it.spanSizeLookup = recyclerViewSpansLookup }
            }
        }
    }

    private val recyclerViewSpansLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return if (position == 0) 2 else 1
        }
    }


    private fun initViews() {
        binding?.apply {
            rvScreen.addItemDecoration(CancerNewsItemDecoration(requireContext()))
        }
    }

    private fun onCancerNewsItemClicked(cancerNewsPreview: CancerNewsPreview) {
        val intent = Intent(requireContext(), CancerNewsWebActivity::class.java)
        intent.putExtra(CancerNewsWebActivity.EXTRA_URL, cancerNewsPreview.url)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}