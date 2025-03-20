package com.dicoding.asclepius.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.asclepius.domain.common.Resource
import com.dicoding.asclepius.domain.common.StringRes
import com.dicoding.asclepius.domain.data.Repository
import com.dicoding.asclepius.domain.model.CancerNewsPreview
import com.dicoding.asclepius.presentation.uievent.InformationUIEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InformationViewModel @Inject constructor(
    private val repository: Repository
): ViewModel() {

    private val _cancerNews = MutableStateFlow<List<CancerNewsPreview>>(emptyList())
    val cancerNews = _cancerNews
        .onStart { loadCancerNews() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _uiEvent = Channel<InformationUIEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private fun loadCancerNews(){
        viewModelScope.launch {
            when(val resource = repository.getNewsAboutCancer()){
                is Resource.Success -> {
                    _cancerNews.value = resource.data
                    _uiEvent.send(InformationUIEvent.SuccessLoadingInitialData(resource.message))
                }
                is Resource.Error -> {
                    _uiEvent.send(
                        InformationUIEvent.FailedLoadingInitialData(
                            StringRes.Dynamic(resource.e.message.toString())
                        )
                    )
                }
                is Resource.Failure -> {
                    _uiEvent.send(
                        InformationUIEvent.FailedLoadingInitialData(
                            StringRes.Dynamic(resource.message.toString())
                        )
                    )
                }
            }
        }
    }

}