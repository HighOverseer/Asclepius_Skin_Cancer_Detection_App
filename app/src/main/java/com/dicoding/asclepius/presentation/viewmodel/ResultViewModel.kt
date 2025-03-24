package com.dicoding.asclepius.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.asclepius.domain.data.Repository
import com.dicoding.asclepius.domain.model.ModelOutput
import com.dicoding.asclepius.domain.model.PredictionHistory
import com.dicoding.asclepius.presentation.uievent.ResultUIEvent
import com.dicoding.asclepius.presentation.uistate.ResultUIState
import com.dicoding.asclepius.presentation.utils.getCurrentDateToString
import com.dicoding.asclepius.presentation.view.ResultActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: Repository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUIState())
    val uiState: StateFlow<ResultUIState> = _uiState

    private val _uiEvent = Channel<ResultUIEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    var latestModelOutput: ModelOutput? = savedStateHandle[ResultActivity.EXTRA_OUTPUT]
    var latestImageUri: String? = savedStateHandle[ResultActivity.EXTRA_URI]
    var isSaveAble: Boolean = savedStateHandle[ResultActivity.EXTRA_SAVEABLE] ?: false

    var latestSessionDate: String? = savedStateHandle[ResultActivity.EXTRA_DATE]
    var latestSessionName: String? = savedStateHandle[ResultActivity.EXTRA_SESSION_NAME]
    var latestSessionNote: String? = savedStateHandle[ResultActivity.EXTRA_NOTE]

    init {
        if (latestModelOutput == null || latestImageUri == null) {
            sendEvent(ResultUIEvent.FailedGettingProperExtras)
        }

        if (!isSaveAble) {
            if (latestSessionDate == null || latestSessionName == null) {
                sendEvent(ResultUIEvent.FailedGettingProperExtras)
            }
        }
    }

    fun sendEvent(event: ResultUIEvent) {
        when (event) {
            ResultUIEvent.ShowSessionDialog -> {
                _uiState.update {
                    it.copy(
                        isShowingDialog = true
                    )
                }
            }

            ResultUIEvent.SessionDialogCanceled -> {
                _uiState.update {
                    it.copy(
                        isShowingDialog = false
                    )
                }
            }

            else -> {}
        }

        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    fun insertPredictionHistory(sessionName: String, note: String) {
        val output = latestModelOutput
        val imageUri = latestImageUri

        if (output == null || imageUri == null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isShowingDialog = false
                )
            }

            repository.insertPredictionHistory(
                PredictionHistory(
                    imageUri = imageUri,
                    modelOutput = output,
                    date = getCurrentDateToString(),
                    sessionName = sessionName,
                    note = note
                )
            )

            sendEvent(ResultUIEvent.SuccessSavingHistory)
        }
    }


}